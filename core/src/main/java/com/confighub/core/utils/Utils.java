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
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.gson.stream.JsonToken.END_DOCUMENT;

public class Utils
{
    private static final Logger log = LogManager.getLogger(Utils.class);

    private static final Pattern namePattern = Pattern.compile("[a-zA-Z0-9]+([\\-\\._]{0,1}[a-zA-Z0-9]+)*+");

    public static boolean isNameValid(String n)
    {
        if (Utils.isBlank(n))
            return false;
        if (n.length() > 120)
            return false;
        return namePattern.matcher(n).matches();
    }

    private static final Pattern componentNamePattern = Pattern.compile("[a-zA-Z0-9]+([\\-\\._+]{0,}[a-zA-Z0-9]+)*+");

    public static boolean isComponentNameValid(String n)
    {
        if (null == n)
            return false;
        return componentNamePattern.matcher(n).matches();
    }

    public final static String keyPatternRegEx = "[\\[\\]\\(\\)\\*\\-\\._+a-zA-Z0-9]+";
    private static final Pattern keyPattern = Pattern.compile(keyPatternRegEx);

    public static boolean isKeyValid(String n)
    {
        if (isBlank(n))
            return false;
        if (n.length() > 120)
            return false;
        return keyPattern.matcher(n).matches();
    }

    public final static String filePatternRegEx = "^(.*/)?(?:$|(.+?)(?:(\\.[^.]*$)|$))";
    private static final Pattern filePattern = Pattern.compile(filePatternRegEx);

    public static boolean isPathAndFileValid(String n)
    {
        if (isBlank(n))
            return true;
        //        if (n.length() > 120) return false;
        return filePattern.matcher(n).matches();
    }

    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\" +
            ".[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);

    public static boolean isEmailValid(String email)
    {
        if (isBlank(email))
            return false;
        return emailPattern.matcher(email).matches();
    }

    public static boolean passwordRequirementsSatisfied(String pass)
    {
        if (Utils.isBlank(pass))
            return false;

        int len = pass.length();
        if (len < 8 || len > 16)
            return false;

        if (-1 != pass.indexOf(' '))
            return false;
        return true;
    }


    public static boolean isBlank(String s)
    {
        if (null == s)
            return true;

        if ("".equals(s.trim()))
            return true;

        return false;
    }

    public static boolean anyBlank(String... ss)
    {
        if (null == ss || ss.length == 0)
        {
            return true;
        }

        for (String s : ss)
        {
            if (isBlank(s))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean allNull(Object... objects)
    {
        for (Object o : objects)
        {
            if (null != o)
            {
                return false;
            }
        }
        return true;
    }

    public static boolean anyNull(Object... objects)
    {
        for (Object o : objects)
        {
            if (null == o)
            {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean equal(T a, T b)
    {
        if (a instanceof String && b instanceof String)
        {
            String _a = (String)a;
            String _b = (String)b;

            if (isBlank(_a) && isBlank(_b))
            {
                return true;
            }
        }

        if (null == a)
        {
            return null == b;
        }

        return a.equals(b);
    }

    /**
     * Returns the plural form of the specified word if cardinality
     * != 1.  The plural form is is generated by concatenating the letter
     * 's' to the end of the word, unless the word ends in an h, s, or x.
     * In these cases, 'es' is added to form the plural.
     * <p>
     * This method assumes that the word is not already plural.
     */
    public static String plural(String word, Number cardinality)
    {
        String suffix = "s";
        char lastChar = word.charAt(word.length() - 1);

        if (lastChar == 'h' || lastChar == 's' || lastChar == 'x')
        {
            suffix = "es";
        }

        return plural(word, suffix, cardinality);
    }

    public static String plural(String word, String suffix, Number cardinality)
    {
        if (cardinality.doubleValue() == 1)
        {
            return word;
        }
        return word + suffix;
    }

    /**
     * Joins the list members into a delimiter separated String.
     *
     * @param list
     * @param delimiter
     * @return
     */
    public static <T> String join(Collection<T> list, String delimiter)
    {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (T line : list)
        {
            sb.append(line);
            if (list.size() != index + 1)
            {
                sb.append(delimiter);
            }
            index++;
        }

        return sb.toString();
    }

    public static <T> String join(T[] list, String delimiter)
    {
        if (null == list)
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (int i = 0; i < list.length; i++)
        {
            String line = list[i].toString();
            sb.append(line.trim());
            if (list.length != index + 1)
            {
                sb.append(delimiter);
            }
            index++;
        }

        return sb.toString();
    }

    public static String capitalizeProperNoun(String s)
    {
        if (isBlank(s))
        {
            return s;
        }

        String[] words = s.split(" ");
        List<String> out = new ArrayList<>();

        for (String word : words)
        {
            if (isBlank(word))
            {
                continue;
            }

            char[] chars = word.toCharArray();
            chars[0] = Character.toUpperCase(chars[0]);

            for (int i = 1; i < chars.length; i++)
            {
                chars[i] = Character.toLowerCase(chars[i]);
            }

            out.add(new String(chars));
        }

        return join(out, " ");
    }

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c)
    {
        List<T> list = new ArrayList<>(c);
        Collections.sort(list);
        return list;
    }

    public static <T> boolean same(T a, T b)
    {
        if (null == a && null == b)
            return true;

        if (null == a)
            return false;

        return a.equals(b);
    }

    public static List<String> split(String s, String delimiter)
    {
        List<String> ret = new ArrayList<>();
        if (isBlank(s))
        {
            return ret;
        }

        String ss[] = s.split(delimiter);
        for (String ass : ss)
        {
            if (isBlank(ass))
            {
                continue;
            }
            ret.add(ass.trim());
        }

        return ret;
    }

    // Return a string like s, but with all c's removed.

    public static String remove(String s, char c)
    {
        StringBuilder copy = null;
        for (int i = 0; i < s.length(); i++)
        {
            if (s.charAt(i) == c)
            {
                if (copy == null)
                {
                    copy = new StringBuilder();
                    copy.append(s, 0, i);
                }
                continue;
            }
            if (copy != null)
            {
                copy.append(s.charAt(i));
            }
        }
        return copy != null ? copy.toString() : s;
    }

    // Return a string like s, but with all characters from 'bad' removed.

    public static String remove(String s, String bad)
    {
        StringBuilder copy = null;
        for (int i = 0; i < s.length(); i++)
        {
            if (bad.contains(s.substring(i, i + 1)))
            {
                if (copy == null)
                {
                    copy = new StringBuilder();
                    copy.append(s, 0, i);
                }
                continue;
            }
            if (copy != null)
            {
                copy.append(s.charAt(i));
            }
        }
        return copy != null ? copy.toString() : s;
    }

    public static <T> boolean same(Collection<T> a, Collection<T> b)
    {
        if (null == a && null == b)
            return true;

        if (null == a || null == b)
            return false;

        if (a.size() != b.size())
            return false;

        for (T el : a)
            if (!b.contains(el))
                return false;

        return true;
    }

    public static String jsonString(String string)
    {
        if (null == string)
            return "";
        return string;
    }

    public static String jsonString(Enum s)
    {
        if (null == s)
            return "";
        return s.name();
    }


    public static String jsonMapToText(String jsonMap)
            throws ConfigException
    {
        try
        {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> json = gson.fromJson(jsonMap, type);

            List<String> lines = json.keySet().stream().map(k -> String.format("%s :: %s", k, json.get(k))).collect(
                    Collectors.toList());
            return join(lines, "\r\n"); // ToDo: should be \n
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    public static String jsonMapToJsonList(String jsonMap)
            throws ConfigException
    {
        try
        {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> json = gson.fromJson(jsonMap, type);

            JsonArray arr = new JsonArray();
            json.keySet().stream().forEach(k -> arr.add(String.format("%s :: %s", k, json.get(k))));

            return gson.toJson(arr);
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    public static String jsonListToText(String jsonList)
            throws ConfigException
    {
        try
        {
            JsonArray json = new Gson().fromJson(jsonList, JsonArray.class);

            List<String> lines = new ArrayList<>();
            for (int i = 0; i < json.size(); i++)
                lines.add(json.get(i).getAsString());

            return join(lines, "\r\n");  // ToDo: should be \n
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    public static String textToJsonMap(String text)
            throws ConfigException
    {
        try
        {
            BufferedReader bufReader = new BufferedReader(new StringReader(text));
            JsonObject json = new JsonObject();

            String line = null;
            while ((line = bufReader.readLine()) != null)
                parseLineForMap(json, line);

            Gson gson = new Gson();
            return gson.toJson(json);
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }


    public static String jsonListToJsonMap(String jsonList)
            throws ConfigException
    {
        try
        {
            Gson gson = new Gson();

            JsonArray json = gson.fromJson(jsonList, JsonArray.class);
            JsonObject toJson = new JsonObject();

            for (int i = 0; i < json.size(); i++)
                parseLineForMap(toJson, json.get(i).getAsString());

            return gson.toJson(toJson);
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    private static void parseLineForMap(JsonObject json, String line)
            throws ConfigException
    {
        if (Utils.isBlank(line))
            return;

        String[] pair = line.split(":", 2);
        if (null == pair || pair.length != 2 || Utils.isBlank(pair[0]))
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);

        json.addProperty(pair[0].trim(), jsonString(pair[1].trim()));
    }

    public static String textToJsonList(String text)
            throws ConfigException
    {
        try
        {
            JsonArray json = new JsonArray();
            json.add(text);

            Gson gson = new Gson();
            return gson.toJson(json);
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    public static String textToBoolean(String text)
            throws ConfigException
    {
        try
        {
            if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false"))
                return Boolean.valueOf(text).toString();

            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    public static String textToInteger(String text)
            throws ConfigException
    {
        try
        {
            return Integer.valueOf(text).toString();
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    public static String textToDouble(String text)
            throws ConfigException
    {
        try
        {
            return Double.valueOf(text).toString();
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    public static String textToFloat(String text)
            throws ConfigException
    {
        try
        {
            return Float.valueOf(text).toString();
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    public static String textToLong(String text)
            throws ConfigException
    {
        try
        {
            return Long.valueOf(text).toString();
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.VALUE_DATA_TYPE_CONVERSION);
        }
    }

    public static String cleanPath(String path)
    {
        if (!Utils.isBlank(path))
        {
            path = path.replaceAll("(/)\\1+", "/");

            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1);
        }

        return path;
    }


    public static String convertGlobToRegEx(String line)
    {
        line = line.trim();
        int strLen = line.length();
        StringBuilder sb = new StringBuilder(strLen);

        boolean escaping = false;
        int inCurlies = 0;
        for (char currentChar : line.toCharArray())
        {
            switch (currentChar)
            {
                case '*':
                    if (escaping)
                        sb.append("\\*");
                    else
                        sb.append(".*");
                    escaping = false;
                    break;
                case '?':
                    if (escaping)
                        sb.append("\\?");
                    else
                        sb.append('.');
                    escaping = false;
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    sb.append('\\');
                    sb.append(currentChar);
                    escaping = false;
                    break;
                case '\\':
                    if (escaping)
                    {
                        sb.append("\\\\");
                        escaping = false;
                    } else
                        escaping = true;
                    break;
                case '{':
                    if (escaping)
                    {
                        sb.append("\\{");
                    } else
                    {
                        sb.append('(');
                        inCurlies++;
                    }
                    escaping = false;
                    break;
                case '}':
                    if (inCurlies > 0 && !escaping)
                    {
                        sb.append(')');
                        inCurlies--;
                    } else if (escaping)
                        sb.append("\\}");
                    else
                        sb.append("}");
                    escaping = false;
                    break;
                case ',':
                    if (inCurlies > 0 && !escaping)
                    {
                        sb.append('|');
                    } else if (escaping)
                        sb.append("\\,");
                    else
                        sb.append(",");
                    break;
                default:
                    escaping = false;
                    sb.append(currentChar);
            }
        }

        return sb.toString();
    }

    public static boolean isJSONValid(final String json)
        throws ConfigException
    {
        try
        {
            return isJsonValid(new StringReader(json));
        }
        catch (Exception e)
        {
            throw new ConfigException(Error.Code.INVALID_JSON_FORMAT);
        }
    }

    private static boolean isJsonValid(final Reader reader)
            throws IOException {
        return isJsonValid(new JsonReader(reader));
    }

    private static boolean isJsonValid(final JsonReader jsonReader)
            throws IOException {
        try {
            JsonToken token;
            while ( (token = jsonReader.peek()) != END_DOCUMENT && token != null ) {
                skipToken(jsonReader);
            }
            return true;
        } catch ( final MalformedJsonException ignored ) {
            return false;
        }
    }

    // Maybe skipToken will be a part of Gson someday: https://github.com/google/gson/issues/1054
    private static void skipToken(final JsonReader reader)
            throws IOException {
        final JsonToken token = reader.peek();
        switch ( token ) {
            case BEGIN_ARRAY:
                reader.beginArray();
                break;
            case END_ARRAY:
                reader.endArray();
                break;
            case BEGIN_OBJECT:
                reader.beginObject();
                break;
            case END_OBJECT:
                reader.endObject();
                break;
            case NAME:
                reader.nextName();
                break;
            case STRING:
            case NUMBER:
            case BOOLEAN:
            case NULL:
                reader.skipValue();
                break;
            case END_DOCUMENT:
            default:
                throw new AssertionError(token);
        }
    }
}
