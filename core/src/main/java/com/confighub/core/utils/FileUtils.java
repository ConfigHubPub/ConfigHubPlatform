/*
 * This file is part of ConfigHub.
 *
 * ConfigHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.confighub.core.utils;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.repository.Property;
import com.confighub.core.repository.PropertyKey;
import com.confighub.core.repository.RepoFile;
import com.confighub.core.resolver.Context;
import com.confighub.core.security.Encryption;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.MultivaluedMap;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.confighub.core.utils.Utils.keyPatternRegEx;

public class FileUtils
{
    private static final Logger log = LogManager.getLogger(FileUtils.class);
    private static final Pattern keyNoDefault = Pattern.compile("(?<!\\\\)\\$\\{\\s*(" + keyPatternRegEx + ")\\s*}");

    public static Collection<String> getKeys(final String text)
    {
        Collection<String> keys = new HashSet<>();
        Matcher m = keyNoDefault.matcher(text);
        while (m.find())
            keys.add(m.group(1).trim());

        return keys;
    }

    public static String resolveFile(final Context context,
                                     final RepoFile file,
                                     Map<PropertyKey, Property> resolved,
                                     Map<String, String> passwords)
            throws ConfigException
    {
        boolean encrypt = false;
        String pass = "";
        if (file.isEncrypted())
        {
            pass = passwords.get(file.getSecurityProfile().getName());
            if (null == pass || !file.getSecurityProfile().isSecretValid(pass))
            {
                pass = file.getSecurityProfile().getDecodedPassword();
                encrypt = true;
            }

            file.decryptFile(pass);
        }

        Map<String, Property> propertyMap = new HashMap<>();
        Map<String, PropertyKey> keyMap = new HashMap<>();
        resolved.keySet().forEach(k -> {
            propertyMap.put(k.getKey(), resolved.get(k));
            keyMap.put(k.getKey(), k);
        });

        String fileContent = file.getContent();
        String patternString = "(?i)\\$\\{\\s*\\b(" + StringUtils.join(propertyMap.keySet(), "|") + ")\\b\\s*}";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(fileContent);

        StringBuffer sb = new StringBuffer();

        while (matcher.find())
        {
            String key = matcher.group(1);
            Property property = propertyMap.get(key);

            // replace each key specification with the property value
            String value = "";

            if (null != property)
            {
                if (null != property.getAbsoluteFilePath() &&
                    PropertyKey.ValueDataType.FileEmbed.equals(property.getPropertyKey().getValueDataType()))
                {
                    RepoFile injectFile = context.resolveFullContextFilePath(property.getAbsoluteFilePath());
                    if (null == injectFile)
                        value = "[ ERROR: No file resolved ]";
                    else
                        value = resolveFile(context, injectFile, resolved, passwords);
                } else
                {
                    if (property.isEncrypted())
                    {
                        String spName = keyMap.get(key).getSecurityProfile().getName();
                        if (passwords.containsKey(spName))
                            property.decryptValue(passwords.get(spName));
                    }
                    value = setValue(property);
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(sb);
        fileContent = sb.toString();

        // Remove all escapes \${...}
        fileContent = fileContent.replaceAll("\\\\\\$\\{", "\\$\\{");

        if (encrypt)
            fileContent = Encryption.encrypt(file.getSecurityProfile().getCipher(), fileContent, pass);

        return fileContent;
    }


    private static String setValue(final Property property)
    {
        String value = property.getValue();

        if (null == value)
        {
            PropertyKey.ValueDataType vdt = property.getPropertyKey().getValueDataType();

            if (PropertyKey.ValueDataType.FileEmbed.equals(vdt)
                || PropertyKey.ValueDataType.FileRef.equals(vdt))
                return "";
            else
                return "null";
        }

        return value.replace("\\", "\\\\");
    }

    public static String previewFile(final Context context,
                                     String fileContent,
                                     Map<String, Property> resolved,
                                     MultivaluedMap<String, String> passwords)
    {
        return previewFile(context, fileContent, resolved, passwords, new HashSet<>());
    }

    private static String previewFile(final Context context,
                                      String fileContent,
                                      Map<String, Property> resolved,
                                      MultivaluedMap<String, String> passwords,
                                      Set<Long> breadcrumbs)
    {
        Collection<String> keyStrings = FileUtils.getKeys(fileContent);

        String patternString = "(?i)\\$\\{\\s*\\b(" + StringUtils.join(keyStrings, "|") + ")\\b\\s*}";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(fileContent);

        StringBuffer sb = new StringBuffer();


        while (matcher.find())
        {
            String key = matcher.group(1);
            Property property = resolved.get(key);

            // replace each key specification with the property value
            String value = "";

            if (null != property)
            {
                if (null != property.getAbsoluteFilePath() &&
                    PropertyKey.ValueDataType.FileEmbed.equals(property.getPropertyKey().getValueDataType()))
                {
                    if (breadcrumbs.contains(property.getAbsoluteFilePath().getId()))
                    {
                        JsonObject json = property.toJson();
                        json.addProperty("key", key);
                        throw new ConfigException(Error.Code.FILE_CIRCULAR_REFERENCE, json);
                    }

                    breadcrumbs.add(property.getAbsoluteFilePath().getId());

                    RepoFile injectFile = context.resolveFullContextFilePath(property.getAbsoluteFilePath());
                    if (null == injectFile)
                        value = "[ ERROR: No file resolved ]";
                    else
                        value = previewFile(context, injectFile.getContent(), resolved, passwords, breadcrumbs);
                }
                else
                {
                    if (property.isEncrypted())
                    {
                        String spName = property.getPropertyKey().getSecurityProfile().getName();
                        if (null != passwords && passwords.containsKey(spName))
                        {
                            String password = passwords.get(spName).get(0);
                            if (!Utils.isBlank(password))
                                property.decryptValue(password);
                        }
                    }
                    value = setValue(property);
                }
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(sb);
        fileContent = sb.toString();

        // Remove all escapes \${...}
        fileContent = fileContent.replaceAll("\\\\\\$\\{", "\\$\\{");
        return fileContent;
    }


    public static String replaceKey(final String content,
                                    final String oldKey,
                                    final String newKey)
    {
        if (Utils.isBlank(content))
            return "";

        String[] lines = content.split(System.getProperty("line.separator"));
        List<String> updated = new ArrayList<>();

        for (String line : lines)
        {
            String tmp = line.replaceAll("(?i)\\$\\{\\s*\\b" + oldKey + "\\b\\s*}",
                                         String.format("\\$\\{ %s \\}", newKey));

            updated.add(tmp);
        }

        return Utils.join(updated, System.getProperty("line.separator"));
    }
}
