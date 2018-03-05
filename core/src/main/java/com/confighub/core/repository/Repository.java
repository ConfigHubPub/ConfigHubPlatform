/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.repository;

import com.confighub.core.error.ConfigException;
import com.confighub.core.error.Error;
import com.confighub.core.organization.Organization;
import com.confighub.core.organization.Team;
import com.confighub.core.rules.AccessRule;
import com.confighub.core.rules.AccessRuleWrapper;
import com.confighub.core.security.SecurityProfile;
import com.confighub.core.security.Token;
import com.confighub.core.store.APersisted;
import com.confighub.core.store.diff.RepositoryDiffTracker;
import com.confighub.core.user.Account;
import com.confighub.core.user.UserAccount;
import com.confighub.core.utils.Utils;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.persistence.*;
import java.util.*;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(
        uniqueConstraints=@UniqueConstraint(columnNames = {"name", "accountId"}),
        indexes = {@Index(name = "REPO_repoIndex", columnList = "id, name, accountId")}
)
@NamedQueries(
{
    @NamedQuery(name = "Level.byId",
                query = "SELECT l FROM Level l WHERE repository=:repository AND id=:id"),

    @NamedQuery(name = "repository.get",
                query = "SELECT c FROM Repository c WHERE id=:id"),

    @NamedQuery(name = "Repository.getByAccount",
                query = "SELECT p FROM Repository p WHERE name=:name AND account=:account AND active=true"),

    @NamedQuery(name = "Repository.count", query = "SELECT COUNT(c) FROM Repository c WHERE c.active=true"),

    @NamedQuery(name = "Repository.getAll", query = "SELECT r FROM Repository r WHERE r.active=true")
})
@EntityListeners({ RepositoryDiffTracker.class })
@Audited
public class Repository
        extends APersisted
{
    private static final Logger log = LogManager.getLogger(Repository.class);

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private boolean active;

    @Column(name = "demo")
    private boolean demo = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Depth depth;

    @Column(nullable = false)
    private String depthLabels;

    @NotAudited
    @Column(nullable = false)
    private Date createDate;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.REFRESH })
    @JoinColumn(name="accountId")
    private Account account;

    @Column(name = "adminContextControlled",
            columnDefinition = "BIT",
            length = 1)
    private boolean adminContextControlled = false;

    @Column(name = "accessControlEnabled")
    private boolean accessControlEnabled;

    @Column(name = "valueTypeEnabled")
    private boolean valueTypeEnabled;

    @Column(name = "securityProfilesEnabled")
    private boolean securityProfilesEnabled;

    @Column(name = "contextClustersEnabled")
    private boolean contextClustersEnabled;

    @Column(name = "allowTokenFreeAPIPush",
            columnDefinition = "BIT",
            length = 1)
    private boolean allowTokenFreeAPIPush;

    @Column(name = "allowTokenFreeAPIPull",
            columnDefinition = "BIT",
            length = 1)
    private boolean allowTokenFreeAPIPull;

    @NotAudited
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private Set<Team> teams;

    @NotAudited
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, mappedBy = "repository")
    private Set<PropertyKey> keys;

    @NotAudited
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, mappedBy = "repository")
    private Set<Token> tokens;

    @NotAudited
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, mappedBy = "repository")
    private Set<Level> levels;

    @NotAudited
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, mappedBy = "repository")
    private Set<SecurityProfile> sps;

    @NotAudited
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, mappedBy = "repository")
    private Set<Property> properties;

    @NotAudited
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, mappedBy = "repository")
    private Set<Tag> tags;

    @NotAudited
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, mappedBy = "repository")
    private Set<RepoFile> files;

    @NotAudited
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, mappedBy = "repository")
    private Set<AbsoluteFilePath> absoluteFilePaths;

    @Column(nullable = false)
    private boolean isPrivate = true;

    // --------------------------------------------------------------------------------------------
    // Construction
    // --------------------------------------------------------------------------------------------
    protected Repository() {}

    public Repository(final String name,
                      final Depth depth,
                      final boolean isPrivate,
                      final Account account)
    {
        this.name = name;
        this.depth = depth;
        this.isPrivate = isPrivate;
        this.account = account;
        this.active = true;
        this.account.joinRepository(this);

        String[] labels = new String[depth.getIndex()];
        for (Depth d : depth.getDepths())
            labels[d.getIndex()-1] = d.name();
        this.depthLabels = Utils.join(labels, ",");

        this.allowTokenFreeAPIPull = false;
        this.allowTokenFreeAPIPush = false;
    }

    // --------------------------------------------------------------------------------------------
    // User/organization management
    // --------------------------------------------------------------------------------------------

    public enum UserType {
        owner(3),
        admin(2),
        member(1),
        nonMember(0);

        private int level = 0;
        public int getLevel() { return this.level; }
        UserType(int l) {
            this.level = l;
        }
    }

    public UserType getUserType(final UserAccount user)
    {
        if (null == user) return UserType.nonMember;

        if (isPersonal())
        {
            if (this.account.getUser().equals(user))
                return UserType.owner;
        }
        else
        {
            if (this.account.getOrganization().isAdmin(user)) return UserType.admin;
            if (this.account.getOrganization().isOwner(user)) return UserType.owner;
        }

        if (null == this.teams)
            return UserType.nonMember;

        for (Team team : this.teams)
            if (team.isMember(user))
                return UserType.member;

        return UserType.nonMember;
    }

    public boolean isPersonal() {
        return account.isPersonal();
    }

    public boolean isEditableBy(UserAccount user)
    {
        if (null == user) return false;

        if (isPersonal() && this.account.getUser().equals(user))
            return true;

        if (!isPersonal())
        {
            if (this.account.getOrganization().isAdmin(user)) return true;
            if (this.account.getOrganization().isOwner(user)) return true;
        }

        return false;
    }

    public void removeTeam(final Team team)
    {
        if (Utils.anyNull(this.teams, team))
            return;

        Iterator<Team> itt = this.teams.iterator();
        while(itt.hasNext())
        {
            Team t = itt.next();
            if (t.equals(team))
            {
                itt.remove();
                break;
            }
        }
    }

    public Team getTeamForUser(final UserAccount userAccount)
    {
        if (Utils.anyNull(this.teams, userAccount))
            return null;

        for (Team team : this.teams)
            if (team.isMember(userAccount))
                return team;

        return null;
    }

    public int getUserCount()
    {
        int cnt = 0;
        if (null == this.teams)
            return cnt;

        for (Team team : this.teams)
            cnt += team.getMemberCount();

        return cnt;
    }

    public boolean isAdminOrOwner(UserAccount user)
    {
        if (isPersonal())
            return this.account.getUser().equals(user);

        return this.account.getOrganization().isOwnerOrAdmin(user);
    }

    public boolean hasReadAccess(UserAccount user)
    {
        if (!this.isPrivate)
            return true;

        return hasWriteAccess(user);
    }

    public boolean hasWriteAccess(UserAccount user)
    {
        if (null == user)
            return false;

        if (isPersonal())
        {
            if (this.account.getUser().equals(user))
                return true;
        }
        else {
            if (this.account.getOrganization().isOwnerOrAdmin(user))
                return true;
        }

        if (null == this.teams)
            return false;

        for (Team team : this.teams)
            if (team.isMember(user))
                return true;

        return false;
    }

    public String getAccountName()
    {
        if (isPersonal())
            return this.account.getUser().getUsername();

        return this.account.getOrganization().getAccountName();
    }

    // ToDo optimization for access rule wrapper
    public AccessRuleWrapper getRulesWrapper(final UserAccount user)
    {
        return getRulesWrapper(user, null);
    }

    public AccessRuleWrapper getRulesWrapper(final UserAccount user, final AccessRule.RuleTarget filter)
    {
        Team team = getTeamForUser(user);
        if (null == team)
            return null;

        return new AccessRuleWrapper(team, filter);
    }

    public AccessRuleWrapper getRulesWrapper(final Team team)
    {
        return getRulesWrapper(team, null);
    }

    public AccessRuleWrapper getRulesWrapper(final Team team, final AccessRule.RuleTarget filter)
    {
        if (null == team)
            return null;

        return new AccessRuleWrapper(team, filter);
    }

    public List<AccessRule> getAccessRules(UserAccount user)
    {
        Team team = getTeamForUser(user);
        if (null == team)
            return null;

        return team.getAccessRules();
    }

    // --------------------------------------------------------------------------------------------
    // Teams
    // --------------------------------------------------------------------------------------------

    public Set<Team> getTeams()
    {
        return this.teams;
    }

    public Team getTeam(String teamName)
    {
        if (null == this.teams)
            return null;

        for (Team team : this.teams)
            if (team.getName().equalsIgnoreCase(teamName))
                return team;
        return null;
    }

    public int getTeamCount()
    {
        if (null == this.teams) return 0;
        return this.teams.size();
    }

    public void addTeam(Team team)
            throws ConfigException
    {
        Team existing = getTeam(name);
        if (null != existing)
            throw new ConfigException(Error.Code.NAME_USED);

        if (null == this.teams)
            this.teams = new HashSet<>();

        this.teams.add(team);
    }


    public boolean isMember(UserAccount user)
    {
        if (null == user || null == this.teams)
            return false;

        for (Team t : this.teams)
            if (t.isMember(user)) return true;

        return false;
    }

    public AccessRule deleteRule(Long ruleId)
    {
        if (null == ruleId || !this.accessControlEnabled || null == this.teams)
            return null;

        for (Team team : this.teams)
        {
            AccessRule rule = team.deleteRule(ruleId);
            if (null != rule) return rule;
        }

        return null;
    }

    // --------------------------------------------------------------------------------------------
    // Ownership re-assignment
    // --------------------------------------------------------------------------------------------

    public void transferOwnershipAccount(final Account newAccount, final UserAccount currentOwner)
        throws ConfigException
    {
        if (Utils.anyNull(newAccount, currentOwner))
            throw new ConfigException(Error.Code.USER_ACCESS_DENIED);

        if (this.isPersonal())
        {
            if (!this.account.getUser().equals(currentOwner))
                throw new ConfigException(Error.Code.USER_ACCESS_DENIED);
        }
        else
        {
            if (!this.account.getOrganization().isOwner(currentOwner))
                throw new ConfigException(Error.Code.USER_ACCESS_DENIED);
        }

        this.account.removeRepository(this);
        newAccount.joinRepository(this);
        this.account = newAccount;
    }

    public boolean isOwner(UserAccount user)
    {
        if (this.account.isPersonal())
            return this.account.getUser().equals(user);

        return this.account.getOrganization().isOwner(user);
    }

    public boolean isAdmin(UserAccount user)
    {
        return !this.account.isPersonal() && this.account.getOrganization().isAdmin(user);
    }

    // ToDo send user invitation

    // --------------------------------------------------------------------------------------------
    // Validation
    // --------------------------------------------------------------------------------------------

    @PrePersist
    protected void setCreationDate() {
        // ToDo this is a hack.  Need a better way to get real creation time.
        long ts = System.currentTimeMillis() + 2000;
        this.createDate = new Date(ts);
    }

    /**
     * Name cannot be blank
     *
     * @throws ConfigException
     */
    @PostUpdate
    @PostPersist
    public void enforce()
            throws ConfigException
    {
        if (Utils.isBlank(this.name))
            throw new ConfigException(Error.Code.BLANK_NAME);

        if (!Utils.isNameValid(this.name))
            throw new ConfigException(Error.Code.ILLEGAL_CHARACTERS);
    }


    // --------------------------------------------------------------------------------------------
    // Depth Labels
    // --------------------------------------------------------------------------------------------

    public String getLabel(Depth depth)
    {
        if (null == depth)
            return null;

        return this.depthLabels.split(",")[depth.getIndex()-1];
    }

    public Depth getDepthFromLabel(String label)
    {
        if (Utils.isBlank(label))
            return null;

        int index = 1;
        for (String l : this.depthLabels.split(","))
        {
            if (label.equalsIgnoreCase(l))
                return Depth.getByIndex(index);
            index++;
        }

        return null;
    }

    public void setDepthLabel(Depth depth, String label)
            throws ConfigException
    {
        if (null == depth || Utils.isBlank(label))
            return;

        List<String> labels = new LinkedList<>(Arrays.asList(this.depthLabels.split(",")));
        labels.remove(depth.getIndex()-1);
        labels.add(depth.getIndex()-1, validateLabel(label));
        this.depthLabels = Utils.join(labels, ",");
    }

    public void setDepthLabels(Map<Depth, String> customDepthLabels)
            throws ConfigException
    {
        if (null == customDepthLabels || customDepthLabels.size() != this.depth.getIndex())
            throw new ConfigException(Error.Code.CONTEXT_SCOPE_MISMATCH); // ToDo: Error message

        String[] labels = new String[this.depth.getIndex()];
        int i=this.depth.getIndex()-1;
        for (Depth d : this.depth.getDepths())
            labels[i--] = customDepthLabels.get(d);

        this.depthLabels = Utils.join(labels, ",");
    }

    public Map<Depth, String> getContextLabels()
    {
        Map<Depth, String> labelMap = new HashMap<>();
        for (Depth d : this.depth.getDepths())
            labelMap.put(d, getLabel(d));
        return labelMap;
    }

    private String validateLabel(String label)
        throws ConfigException
    {
        if (!Utils.isNameValid(label))
            throw new ConfigException(Error.Code.ILLEGAL_CHARACTERS);

        return label;
    }

    // --------------------------------------------------------------------------------------------
    // POJO Ops
    // --------------------------------------------------------------------------------------------
    public String getLogId()
    {
        return String.format("[%d] %s/%s", this.getId(), getAccountName(), this.name);
    }

    @Override
    public boolean equals(Object o)
    {
        if (null == o)
            return false;

        return o instanceof Repository && ((Repository)o).getId().equals(this.getId());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.name);
    }

    @Override
    public String toString()
    {
        return String.format("Repository[%5d]: depth: %s, %s %s, %s",
                             this.id,
                             this.depth.name(),
                             this.isPrivate ? "Private" : "Organization",
                             this.name,
                             null == this.account ? "deleted" : this.account.isPersonal() ? "Personal" : "Organization");
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();

        json.addProperty("name", this.name);
        json.addProperty("private", this.isPrivate);
        json.addProperty("account", this.getAccountName());
        json.addProperty("isPersonal", this.isPersonal());

        JsonObject labels = new JsonObject();
        for (Depth depth : this.getDepth().getDepths())
            labels.addProperty(String.valueOf(depth.getPlacement()), this.getLabel(depth));
        json.add("labels", labels);

        return json;
    }

    // --------------------------------------------------------------------------------------------
    // Setters and getters
    // --------------------------------------------------------------------------------------------

    public Long getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Depth getDepth()
    {
        return depth;
    }

    public void setDepth(Depth depth)
    {
        this.depth = depth;
    }

    public Map<Depth, String> getDepthLabels()
    {
        Map<Depth, String> m = new HashMap<>();
        int i = 1;
        for (String l : this.depthLabels.split(","))
            m.put(Depth.getByIndex(i++), l);

        return m;
    }

    public String getLabels() {
        return this.depthLabels;
    }

    public boolean isPrivate()
    {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate)
    {
        this.isPrivate = isPrivate;
    }

    public Date getCreateDate()
    {
        return this.createDate;
    }

    public Account getAccount() {
        return this.account;
    }

    public UserAccount getOwner()
    {
        return this.account.getUser();
    }

    public Organization getOrganization()
    {
        return this.account.getOrganization();
    }

    public boolean isAccessControlEnabled()
    {
        return accessControlEnabled;
    }

    public void setAccessControlEnabled(boolean accessControlEnabled)
    {
        this.accessControlEnabled = accessControlEnabled;
    }

    public boolean isValueTypeEnabled()
    {
        return valueTypeEnabled;
    }

    public void setValueTypeEnabled(boolean valueTypeEnabled)
    {
        this.valueTypeEnabled = valueTypeEnabled;
    }

    public boolean isSecurityProfilesEnabled()
    {
        return securityProfilesEnabled;
    }

    public void setSecurityProfilesEnabled(boolean securityProfilesEnabled)
    {
        this.securityProfilesEnabled = securityProfilesEnabled;
    }

    public boolean isContextClustersEnabled()
    {
        return contextClustersEnabled;
    }

    public boolean canUserManageContext(final UserAccount user)
    {
        if (!this.adminContextControlled)
            return true;

        return isAdminOrOwner(user);
    }

    public boolean isAdminContextControlled()
    {
        return this.adminContextControlled;
    }

    public void setAdminContextControlled(boolean adminContextControlled)
    {
        this.adminContextControlled = adminContextControlled;
    }

    public void setContextClustersEnabled(boolean contextClustersEnabled)
    {
        this.contextClustersEnabled = contextClustersEnabled;
    }

    public Set<PropertyKey> getKeys()
    {
        return keys;
    }

    public Set<RepoFile> getFiles()
    {
        return files;
    }

    public Set<AbsoluteFilePath> getAbsoluteFilePaths()
    {
        return absoluteFilePaths;
    }

    public Set<Token> getTokens()
    {
        return tokens;
    }

    public Token getToken(final String tokenString) {
        if (Utils.isBlank(tokenString) || null == this.tokens)
            return null;

        for (Token token : this.tokens)
            if (token.getToken().equals(tokenString))
                return token;

        return null;
    }

    public Set<Level> getLevels()
    {
        return levels;
    }

    public Set<SecurityProfile> getSps()
    {
        return sps;
    }

    public Set<Property> getProperties()
    {
        return properties;
    }

    public boolean isActive()
    {
        return active;
    }

    public boolean isDemo()
    {
        return demo;
    }

    public void setDemo(boolean demo)
    {
        this.demo = demo;
    }

    public Set<Tag> getTags()
    {
        return tags;
    }

    public void destroy()
    {
        if (null != this.teams && this.teams.size() > 0)
        {
            Iterator<Team> teamIterator = this.teams.iterator();
            while(teamIterator.hasNext())
            {
                Team t = teamIterator.next();
                if (null != t.getMembers())
                {
                    Iterator<UserAccount> members = t.getMembers().iterator();
                    while(members.hasNext())
                    {
                        UserAccount u = members.next();
                        members.remove();
                        t.removeMember(u);
                    }
                }
            }
        }

        this.active = false;
        this.account.removeRepository(this);
        this.account = null;
    }

    public boolean isAllowTokenFreeAPIPush()
    {
        return allowTokenFreeAPIPush;
    }
    public boolean isAllowTokenFreeAPIPull()
    {
        return allowTokenFreeAPIPull;
    }

    public void setAllowTokenFreeAPIPush(boolean allowTokenFreeAPIPush)
    {
        this.allowTokenFreeAPIPush = allowTokenFreeAPIPush;
    }

    public void setAllowTokenFreeAPIPull(boolean allowTokenFreeAPIPull)
    {
        this.allowTokenFreeAPIPull = allowTokenFreeAPIPull;
    }

    public boolean isDeleted() { return null == this.account; }

    @Override
    public ClassName getClassName() {
        return ClassName.Repository;
    }

}
