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

package com.confighub.core.store.diff;

import com.confighub.core.security.CipherTransformation;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.store.APersisted;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.*;

public class SecurityProfileDiffTracker
        extends ADiffTracker
{
    private static final Logger log = LogManager.getLogger(SecurityProfileDiffTracker.class);

    @PreRemove
    public void preRemove(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Delete;
        markForNotification();
    }

    @PrePersist
    public void prePersist(APersisted obj)
    {
        obj.revType = APersisted.RevisionType.Add;
    }

    @PostPersist
    public void postPersist(APersisted obj)
    {
        markForNotification();
    }


    @PostLoad
    public void loadOldData(APersisted obj)
    {
        if (!isTracked() || !(obj instanceof SecurityProfile))
            return;

        OriginalSecurityProfile o = new OriginalSecurityProfile();
        SecurityProfile sp = (SecurityProfile) obj;

        o.name = sp.getName();
        o.password = sp.getSpPassword();
        o.cipher = sp.getCipher();
        o.encrypted = sp.encryptionEnabled();

        obj.revType = APersisted.RevisionType.Modify;
        edits.get().put(obj.getId(), o);
    }

    @PreUpdate
    public void preUpdate(APersisted obj)
    {
        OriginalSecurityProfile o = (OriginalSecurityProfile) getIfRecorded(obj);
        if (null == o || !(obj instanceof SecurityProfile))
            return;

        SecurityProfile sp = (SecurityProfile) obj;
        JsonObject json = new JsonObject();

        if (!Utils.equal(sp.getName(), o.name))
            json.addProperty("name", o.name);

        if (!Utils.equal( sp.getSpPassword(), o.password))
            json.addProperty("password", true);

        if (sp.getCipher() != o.cipher)
            json.addProperty("cipher", null == o.cipher ? "" : o.cipher.getName());

        if (sp.encryptionEnabled() != o.encrypted)
            json.addProperty("encrypted", o.encrypted);

        markForNotification();
        sp.diffJson = json.toString();
    }

    private static class OriginalSecurityProfile
            extends OriginalAPersistent
    {
        String name;
        String password;
        boolean encrypted;
        CipherTransformation cipher;
    }
}
