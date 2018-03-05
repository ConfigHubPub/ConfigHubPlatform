/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.store;

import com.confighub.core.utils.DateTimeUtils;
import com.confighub.core.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.*;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Revision information for each audit revision
 */
@Entity
@NamedQueries(
{
    @NamedQuery(name = "RevisionEntry.get",
            query = "SELECT r FROM RevisionEntry r WHERE repositoryId=:repositoryId AND id=:id"),

    @NamedQuery(name = "Repository.first",
        query = "SELECT r FROM RevisionEntry r WHERE repositoryId=:repositoryId " +
                "AND commitGroup IN :commitGroupList " +
                "ORDER BY id DESC"),

    @NamedQuery(name = "Repository.forward",
        query = "SELECT r FROM RevisionEntry r WHERE repositoryId=:repositoryId " +
                "AND commitGroup IN :commitGroupList " +
                "AND id<:id " +
                "ORDER BY id DESC"),

    @NamedQuery(name = "Repository.back",
        query = "SELECT r FROM RevisionEntry r WHERE repositoryId=:repositoryId " +
                "AND commitGroup IN :commitGroupList " +
                "AND id>:id " +
                "ORDER BY id ASC"),

    @NamedQuery(name = "Repository.last",
        query = "SELECT r FROM RevisionEntry r WHERE repositoryId=:repositoryId " +
                "AND commitGroup IN :commitGroupList " +
                "ORDER BY id ASC"),

    // Notifications
    @NamedQuery(name = "Notifications.first",
        query = "SELECT r FROM RevisionEntry r WHERE repositoryId=:repositoryId AND notify=true " +
                "ORDER BY id DESC"),

    @NamedQuery(name = "Notifications.forward",
        query = "SELECT r FROM RevisionEntry r WHERE repositoryId=:repositoryId AND notify=true " +
                "AND id<:id " +
                "ORDER BY id DESC"),

    @NamedQuery(name = "Notifications.back",
        query = "SELECT r FROM RevisionEntry r WHERE repositoryId=:repositoryId AND notify=true " +
                "AND id>:id " +
                "ORDER BY id ASC"),

    @NamedQuery(name = "Notifications.last",
        query = "SELECT r FROM RevisionEntry r WHERE repositoryId=:repositoryId AND notify=true " +
                "ORDER BY id ASC")

})

@RevisionEntity(RevisionManager.class)
public class RevisionEntry
{
    private static final Logger log = LogManager.getLogger(RevisionEntry.class);

    @Id
    @GeneratedValue
    @RevisionNumber
    private Long id;

    @RevisionTimestamp
    @Column(name = "timestamp")
    private Long timestamp;

    @Column(name = "userId")
    private Long userId;

    @Column(name = "appId")
    private String appId;

    @Column(name = "repositoryId")
    private Long repositoryId;

    @Column(name = "type")
    private String type;

    @Column(name = "notify")
    private boolean notify;

    @Column(length = 10485760, name = "searchKey")
    private String searchKey;

    @Column(length = 10485760)
    private String revType;

    @Enumerated(EnumType.STRING)
    private CommitGroup commitGroup;

    @Column(columnDefinition = "TEXT")
    private String changeComment;

    public enum CommitGroup {
        RepoSettings,
        Config,
        Files,
        Security,
        Tags,
        Tokens,
        Teams
    }

    public void set(RevisionEntityContext rec)
    {
        this.userId = rec.getUserId();
        this.appId = rec.getAppId();
        this.repositoryId = rec.getRepositoryId();
        this.changeComment = rec.getChangeComment();
        this.notify = rec.isNotify();

        Set<APersisted.ClassName> recType = rec.getType();
        JsonObject json = new JsonObject();

        if (rec.isContextResize())
        {
            rec.getRevTypes().forEach((id, revType) -> {
                if (this.repositoryId.equals(id))
                    json.addProperty(id.toString(), revType.name());
            });
            this.commitGroup = CommitGroup.RepoSettings;
        }
        else
        {
            Set<String> searchKeys = rec.getSearchKey();
            if (null != searchKeys && searchKeys.size() > 0)
                this.searchKey = "|" + Utils.join(searchKeys, "|") + "|";

            rec.getRevTypes().forEach((id, revType) -> json.addProperty(id.toString(), revType.name()));

            if (null != recType)
            {
                switch (recType.iterator().next())
                {
                    case Repository:
                        this.commitGroup = CommitGroup.RepoSettings;
                        break;

                    case ContextItem:
                    case Property:
                    case PropertyKey:
                        // if its already a file change - leave it at that.
                        if (CommitGroup.Files.equals(this.commitGroup))
                            break;

                        this.commitGroup = CommitGroup.Config;
                        break;

                    case RepoFile:
                    case AbsoluteFilePath:
                        this.commitGroup = CommitGroup.Files;
                        break;

                    case SecurityProfile:
                        this.commitGroup = CommitGroup.Security;
                        break;

                    case Token:
                        this.commitGroup = CommitGroup.Tokens;
                        break;

                    case Tag:
                        this.commitGroup = CommitGroup.Tags;
                        break;

                    case Team:
                    case AccessRule:
                        this.commitGroup = CommitGroup.Teams;
                        break;
                }
            }
        }

        this.type = Utils.join(recType, ",");
        this.revType = json.toString();
    }

    public long getId()
    {
        return id;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public Long getUserId()
    {
        return userId;
    }

    public long getRepositoryId()
    {
        return repositoryId;
    }

    public String getType()
    {
        return type;
    }

    public String getChangeComment()
    {
        return changeComment;
    }

    public void setChangeComment(String changeComment)
    {
        this.changeComment = changeComment;
    }

    public boolean isNotify()
    {
        return notify;
    }

    public void setNotify(boolean notify)
    {
        this.notify = notify;
    }

    public Map<String, String> getRevTypes(Gson gson)
    {
        Type revTypeMap = new TypeToken<Map<String, String>>() { }.getType();
        return gson.fromJson(this.revType, revTypeMap);
    }

    public CommitGroup getCommitGroup()
    {
        return commitGroup;
    }

    public String getSearchKey()
    {
        return searchKey;
    }

    public String getAppId()
    {
        return appId;
    }

    @Override
    public String toString()
    {
        return String.format("%s | rev# %d | type: %s | userId: %d | appId: %s, repositoryId: %d | %s",
                             DateTimeUtils.standardDTFormatter.get().format(new Date(this.timestamp)),
                             this.id,
                             this.revType,
                             this.userId,
                             this.appId,
                             this.repositoryId,
                             this.commitGroup);
    }
}
