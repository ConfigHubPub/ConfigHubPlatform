/*
 * Copyright (c) 2016 ConfigHub, LLC to present - All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.confighub.core.error;

/**
 * Error codes used though core application.
 */
public abstract class Error
{
    public enum Code
    {
        DATE_API_FORMAT_ERROR("Invalid date format.  Expected 'ISO 8601' format: 'YYYY-MM-DDTHH:MM:SSZ"),

        BLANK_VALUE("Cannot be blank."),
        INVALID_TYPE("Invalid type specified."),
        RESOURCE_NOT_FOUND("Requested resource not found."),
        BLANK_NAME("Name cannot be empty."),
        ILLEGAL_CHARACTERS("Names may only contain alphanumeric characters or single hyphens, and cannot begin or end" +
                           " with a hyphen."),
        NOT_FOUND("Item not found"),
        NAME_USED("Name is already used"),

        // Core System
        CONSTRAINT("A relationship to another item exists, preventing this transaction."),
        CATCH_ALL("Unable to process request."),
        MISSING_PARAMS("Required parameters not specified."),
        AUDITED_NO_CLASS_DEF("Missing class definition for Audited APersisted."),
        DB_LOCKING("Looks like you are competing for the same resource with someone else.  Please try again."),
        INTERNAL_ERROR("We have encountered an error - please try again. If the problem persists please " +
                               "contact support."),

        // User
        CONTEXT_EDIT_DISABLED("You do not have access to manage context elements for this repository."),
        USER_ACCESS_DENIED("You do not have access to requested resource or operation."),
        USER_MAX("Maximum number of users reached."),
        USER_NOT_REGISTERED("Account not found."),
        USER_AUTH("Invalid login credentials specified."),
        USER_BAD_EMAIL("Invalid email specified."),
        EMAIL_REGISTERED("Email is already registered."),
        ACCOUNT_NOT_FOUND("Specified account is not found."),
        ACCOUNT_INVALID("Invalid account specified"),
        PASSWORD_REQUIREMENTS("Password must be between 8-16 characters long with no spaces."),
        PASSWORDS_MISSMATCH("Passwords do not match."),

        // Repository
        REPOSITORY_NOT_FOUND("Repository not found."),
        SECURITY_PROFILES_DISABLED("Security profiles feature is not enabled."),

        // Property
        PROP_DUPLICATION_CONTEXT("Another value with the same context exists."),
        PROP_PARENT_LOCK("A value with a broader context is locked, and is preventing an override."),
        PROP_CHILD_LOCK("This value has an existing override. Locking context is not possible."),
        PROP_CONTEXT_DUPLICATE_DEPTH("Context cannot contain elements with same depth."),
        CONTEXT_EDITING_ACCESS_DENIED("You do not have write access for specified context."),
        ACCESS_NOT_DEFINED("Repository enforces access controls, however your access is not defined.  By default, your access is read-only."),
        LEVEL_EDITING_ACCESS_DENIED("Context item cannot be changed.  It is used by properties you do not have write access to."),

        // Organization
        ORG_NO_OWNERS("You cannot remove the last owner of the organization.  Delete the organization and its repositories instead."),
        ORG_EXISTING_MANAGER("User is already a manager in this organization."),
        ORG_DELETE("Organization cannot be deleted while there are existing repositories."),

        // PropertyKey
        KEY_DUPLICATE("Key already exists."),
        KEY_BLANK("Key cannot be blank."),
        KEY_EDITING_ACCESS_DENIED("You do not have write access to all keys/files that belong to this security group."),
        KEY_USED_BY_FILES("This key is used in at least one file, and cannot be deleted."),
        KEY_USED_BY_VALUES("This key has at least one value, and cannot be deleted using cleanup."),
        PUSH_DISABLED("Push updates are disabled for specified key."),

        // Context Element
        GROUP_CIRCULAR_REF("Group cannot be in its own group set."),
        GROUP_MULTI_DEPTH("Group cannot contain another group."),
        ASSIGNED_PROPERTIES("You cannot delete a context item or rank that is used by properties."),
        CONTEXT_SCOPE_MISMATCH("Context items specified are outside of repository context scope."),
        CLUSTERING_DISABLED("Context-Group feature is not enabled for this repository."),

        // Teams
        TEAM_NOT_FOUND("Requested team cannot be found."),
        MULTIPLE_TEAM_MEMBERSHIPS("User already belongs to another team in the same repository."),

        // Client Request
        PARTIAL_CONTEXT("Specified context not fully qualified."),
        SECURITY_ERROR("Security error occurred while processing request."),
        IO_ERROR("IO Error during client request."),
        INVALID_CLIENT_TOKEN("Invalid API token."),
        EXPIRED_TOKEN("API token has expired."),
        NON_ACTIVE_TOKEN("API token is not active."),
        TOKEN_CANNOT_BE_CHANGED("Tokens cannot be modified."),
        NOT_AUTHORIZED_TO_CHANGE_TOKEN("You are not authorized to modify this token."),
        INVALID_JSON_FORMAT(
                "Posted data is not in expected JSON format.  Consult the API documentation for formatting specifications."),
        KEY_CREATION_VIA_API_DISABLED("Property key creation is disabled by default for API Push requests.  See documentation for options."),

        // File formats
        INVALID_FILE_FORMAT("Invalid file format specified.  Consult API documentation for correct file export formats."),
        FILE_SIZE_MAX_EXCEEDED("Maximum file size is 2MB"),
        FILE_ILLEGAL_CHARACTERS("Path and file name syntax is not correct."),
        MISSING_DIR("Specified directory path does not exist."),
        INVALID_DIR_NAME("You specified an invalid directory name."),
        DIR_NOT_EMPTY("Directory is not empty."),
        FILE_NOT_FOUND("Specified file is not found."),
        FILE_DUPLICATION_CONTEXT("Another file with the same context exists."),
        FILE_REFERENCED_BY_VALUE("Property values reference files."),
        FILE_CIRCULAR_REFERENCE("Circular reference found among files referenced by resolved values of keys defined within the files."),

        // Encryption
        INVALID_CIPHER("Invalid cipher specified."),
        ENCRYPTION_ERROR("Unable to encrypt specified value."),
        ENCRYPTED_FILE("Requested file is encrypted.  Password is required."),
        DECRYPTION_ERROR("Unable to decrypt specified value with provided password."),
        ENCRYPTION_MISMATCH("Encryption profiles between merging keys are not the same."),
        ASSIGNED_TO_SECURE_PROFILE("Key is already assigned to a security-profile."),
        MISSING_SECURITY_PROFILE("Security profile specified is no longer available."),
        INVALID_PASSWORD("Invalid password specified."),

        // Value-Data-Type
        INVALID_VALUE_DATA_TYPE("Invalid value-data-type specified."),
        INVALID_VALUE_FOR_DATA_TYPE("Value specified is not valid format for key specified Value-Data-Type."),
        VALUE_DATA_TYPE_MISMATCH("Value-data-types between merging keys are not the same."),
        VALUE_DATA_TYPE_CONVERSION("Conversion for value-data-type is not possible."),

        // Group
        GROUP_TO_GROUP_ASSIGNMENT("Group cannot be a node of another group."),
        GROUP_CONFLICT("Group-Member conflict."),

        // Level
        FAILED_TO_PARSE_CONTEXT_ELEMENT("Failed to parse specified context element."),
        CONTEXT_NOT_SPECIFIED("Value context not specified."),

        // Token
        INVALID_PASS_CHANGE_TOKEN("This link has either expired or is not valid."),

        // API
        TOKEN_FREE_PULL_ACCESS_DENIED("This repository does not allow API Pull without specifying a token"),
        TOKEN_FREE_PUSH_ACCESS_DENIED("This repository does not allow API Push without specifying a token"),
        TAG_NOT_FOUND("Specified repository tag not found."),

        // Enterprise
        EXPIRED("ConfigHub Licence has either expired, or you have reached the license allowed property limit.  " +
                        "Configuration Pull will continue to work, but all data modification is disabled.")
        ;

        private String message;

        Code(String message)
        {
            this.message = message;
        }

        public String getMessage()
        {
            return message;
        }
    }
}
