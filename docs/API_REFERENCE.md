# ShareFile REST API Reference

**API Version:** v3  
**Base URL:** `https://{subdomain}.sf-api.com/sf/v3`  
**Protocol:** OData v3 (JSON Light)  
**Authentication:** OAuth2 Bearer Token  
**Date:** 2026-05-08  

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [OData Conventions](#2-odata-conventions)
3. [Items](#3-items)
4. [Users](#4-users)
5. [Shares](#5-shares)
6. [Accounts](#6-accounts)
7. [Groups](#7-groups)
8. [AccessControls](#8-accesscontrols)
9. [Zones](#9-zones)
10. [Devices](#10-devices)
11. [WebhookSubscriptions](#11-webhooksubscriptions)
12. [AsyncOperations](#12-asyncoperations)
13. [Models](#13-models)
14. [Enums](#14-enums)

---

## 1. Authentication

### 1.1 OAuth2 Authorization Code Flow

**Step 1: Redirect user to authorize**

```
GET https://secure.sharefile.com/oauth/authorize
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `response_type` | String | Yes | `code` (recommended) or `token` |
| `client_id` | String | Yes | Application client ID |
| `redirect_uri` | String | Yes | HTTPS callback URL |
| `state` | String | Recommended | CSRF prevention token |

**Step 2: Exchange code for token**

```
POST https://{subdomain}.{apicp}/oauth/token
Content-Type: application/x-www-form-urlencoded
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grant_type` | String | Yes | `authorization_code` |
| `code` | String | Yes | Authorization code from redirect |
| `client_id` | String | Yes | Application client ID |
| `client_secret` | String | Yes | Application client secret |

**Response:**

```json
{
  "access_token": "xxxxxx",
  "refresh_token": "xxxxxx",
  "token_type": "bearer",
  "apicp": "sharefile.com",
  "appcp": "sharefile.com",
  "subdomain": "mycompany",
  "expires_in": 28800
}
```

### 1.2 Password Grant

```
POST https://{subdomain}.{apicp}/oauth/token
Content-Type: application/x-www-form-urlencoded
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grant_type` | String | Yes | `password` |
| `username` | String | Yes | User email address |
| `password` | String | Yes | User password |
| `client_id` | String | Yes | Application client ID |
| `client_secret` | String | Yes | Application client secret |

### 1.3 Refresh Token

```
POST https://{subdomain}.{apicp}/oauth/token
Content-Type: application/x-www-form-urlencoded
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grant_type` | String | Yes | `refresh_token` |
| `refresh_token` | String | Yes | Current refresh token |
| `client_id` | String | Yes | Application client ID |
| `client_secret` | String | Yes | Application client secret |

### 1.4 Using the Token

All API requests require the bearer token header:

```
Authorization: Bearer {access_token}
```

### 1.5 HMAC Validation

The authorization code redirect includes an `h` parameter — an HMAC-SHA256 signature of the redirect URI (excluding `h` itself), signed with the client secret. Validate this before exchanging the code.

---

## 2. OData Conventions

### 2.1 URL Structure

```
https://{server}/sf/v3/{Entity}({id})/{Action}
```

- **Server:** `{subdomain}.sf-api.com`
- **Provider:** `sf` (ShareFile.com)
- **Version:** `v3`
- **Entity:** PascalCase resource name (Items, Users, Shares, etc.)
- **Id:** GUID string identifier

### 2.2 Query Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `$select` | Return only specified properties | `$select=Name,Email,CreationDate` |
| `$expand` | Include related entities inline | `$expand=Parent,Zone` or `$expand=*` |
| `$filter` | Filter results | `$filter=Name eq 'Reports'` |
| `$orderby` | Sort results | `$orderby=CreationDate desc` |
| `$top` | Limit result count | `$top=100` |
| `$skip` | Offset for pagination | `$skip=50` |

### 2.3 Filter Operators

| Operator | Example |
|----------|---------|
| `eq` | `$filter=Name eq 'file.txt'` |
| `ne` | `$filter=IsHidden ne true` |
| `gt` / `lt` | `$filter=FileSizeBytes gt 1000000` |
| `substringof` | `$filter=substringof('report', Name)` |
| `startswith` | `$filter=startswith(Name, 'Q1')` |
| `endswith` | `$filter=endswith(Name, '.pdf')` |
| `isof` | `$filter=isof('ShareFile.Api.Models.File')` |
| `and` / `or` | `$filter=IsHidden eq false and FileSizeBytes gt 0` |

### 2.4 Response Format

**Single entity:**

```json
{
  "odata.metadata": "https://account.sf-api.com/sf/v3/$metadata#Items/@Element",
  "odata.type": "ShareFile.Api.Models.Folder",
  "Id": "abc-123",
  "Name": "My Folder",
  ...
}
```

**Collection (Feed):**

```json
{
  "odata.metadata": "https://account.sf-api.com/sf/v3/$metadata#Items",
  "odata.count": 150,
  "odata.nextLink": "https://account.sf-api.com/sf/v3/Items(id)/Children?$skip=100",
  "value": [
    { "Id": "abc-123", "Name": "File1.pdf", ... },
    { "Id": "def-456", "Name": "File2.pdf", ... }
  ]
}
```

### 2.5 HTTP Methods

| Method | Purpose |
|--------|---------|
| `GET` | Read (no side effects) |
| `POST` | Create new entities, trigger actions |
| `PATCH` | Partial update of existing entities |
| `PUT` | Full replacement (rare, used for Roles) |
| `DELETE` | Remove entities |

---

## 3. Items

Items represent files, folders, notes, links, and symbolic links in ShareFile.

**Special Item IDs:** `home`, `favorites`, `allshared`, `connectors`, `box`, `top`

### 3.1 Get Home Folder

```
GET /sf/v3/Items
```

Returns the home folder for the authenticated user.

**Response:** `Item`

---

### 3.2 Get Item by ID

```
GET /sf/v3/Items({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item identifier (or special ID like `home`) |
| `includeDeleted` | Boolean | No | Include deleted items |

**Response:** `Item`

---

### 3.3 Get TreeView

```
GET /sf/v3/Items({id})/TreeView
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Folder ID |
| `treemode` | TreeMode | Yes | `copy`, `move`, or `manage` |
| `sourceId` | String | No | Source item for the operation |
| `canCreateRootFolder` | Boolean | No | Allow root folder creation |
| `fileBox` | Boolean | No | FileBox parameter |

**Response:** Tree root `Item`

---

### 3.4 Get Connector Group Children

```
GET /sf/v3/ConnectorGroups({id})/Children
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Connector group ID |

**Response:** Feed of `SymbolicLink`

---

### 3.5 Get Stream (File Versions)

```
GET /sf/v3/Items({id})/Stream
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Stream ID |
| `includeDeleted` | Boolean | No | Include expired items |

**Response:** Feed of `Item` (file versions)

---

### 3.6 Get Item by Path

```
GET /sf/v3/Items/ByPath
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | String | Yes | Absolute path, e.g. `/folder1/folder2/file.txt` |

**Response:** `Item`

---

### 3.7 Get Item by Relative Path

```
GET /sf/v3/Items({id})/ByPath
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Root item ID |
| `path` | String | Yes | Relative path from root |

**Response:** `Item`

---

### 3.8 Get Parent

```
GET /sf/v3/Items({id})/Parent
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |

**Response:** `Item`

---

### 3.9 Get Children

```
GET /sf/v3/Items({id})/Children
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Folder ID |
| `includeDeleted` | Boolean | No | Include deleted children |
| `orderingMode` | ItemOrderingMode | No | Sort: `FoldersFirst` (default), `DateDesc`, `DateAsc`, `NameAsc`, `NameDesc` |

**Response:** Feed of `Item`

---

### 3.10 Get Folder Access Info

```
GET /sf/v3/Items({id})/Info
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Folder ID |

**Response:** `ItemInfo`

---

### 3.11 Download Item

```
GET /sf/v3/Items({id})/Download
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |
| `redirect` | Boolean | No | `true` = 302 redirect, `false` = DownloadSpecification |
| `includeAllVersions` | Boolean | No | Include old file versions in folder downloads |
| `includeDeleted` | Boolean | No | Include archived items (admin only) |

**Response:** 302 redirect to download URL, or `DownloadSpecification`

---

### 3.12 Get Thumbnail

```
GET /sf/v3/Items({id})/Thumbnail
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |
| `size` | Int32 | No | `75` (default) or `600` pixels |
| `redirect` | Boolean | No | `true` = 302 redirect, `false` = `Redirection` object |

**Response:** 302 redirect or `Redirection`

---

### 3.13 Get Breadcrumbs

```
GET /sf/v3/Items({id})/Breadcrumbs
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Target item ID |

**Response:** Feed of `Item` representing path from root to target

---

### 3.14 Search (Global)

```
GET /sf/v3/Items/Search
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | String | Yes | Search query string |
| `maxResults` | Int32 | No | Maximum results |
| `skip` | Int32 | No | Results to skip |
| `homeFolderOnly` | Boolean | No | Search only user's home folder |

**Response:** `SearchResults`

---

### 3.15 Search (Folder-Specific)

```
GET /sf/v3/Items({id})/Search
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Parent folder ID |
| `query` | String | Yes | Search query string |
| `maxResults` | Int32 | No | Maximum results (default: 50) |
| `skip` | Int32 | No | Results to skip (default: 0) |

**Response:** `SearchResults`

---

### 3.16 Get Web Preview Link

```
GET /sf/v3/Items({id})/WebView
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |

**Response:** `Redirection` to web edit application

---

### 3.17 Get Protocol Links

```
GET /sf/v3/Items({id})/ProtocolLinks({platform})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |
| `platform` | String | Yes | `all`, `web`, `webandmobile`, or `mobile` |

**Response:** List of protocol links

---

### 3.18 Get Redirection Endpoint

```
GET /sf/v3/Items({id})/Redirection
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |

**Response:** `Redirection`

---

### 3.19 Get Deleted Children

```
GET /sf/v3/Items({id})/DeletedChildren
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Parent folder ID |

**Response:** Feed of recoverable/deleted `Item`

---

### 3.20 Get User Deleted Items

```
GET /sf/v3/Items/UserDeletedItems
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userid` | String | Yes | User ID |
| `zone` | String | No | Zone ID filter |

**Response:** Feed of deleted `Item`

---

### 3.21 Get Items by DLP Status

```
GET /sf/v3/Items/ByDlpStatus
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | DlpStatus | Yes | `Unscanned`, `ScannedOK`, or `ScannedRejected` |
| `zone` | String | No | Zone ID |
| `enddate` | DateTime | No | Creation date filter |

**Response:** Feed of `Item`

---

### 3.22 Create Folder

```
POST /sf/v3/Items({parentId})/Folder
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `parentId` | String | Yes | Parent folder ID |
| `overwrite` | Boolean | No | Overwrite existing (default: false) |
| `passthrough` | Boolean | No | Passthrough flag |

**Request Body:**

```json
{
  "Name": "New Folder",
  "Description": "Optional description",
  "Zone": { "Id": "zone-id" },
  "ExpirationDate": "2026-12-31"
}
```

**Response:** `Folder`

---

### 3.23 Create Note

```
POST /sf/v3/Items({parentId})/Note
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `parentId` | String | Yes | Parent folder ID |

**Request Body:**

```json
{
  "Name": "Note Title",
  "Description": "Note content"
}
```

**Response:** `Note`

---

### 3.24 Create Link

```
POST /sf/v3/Items({parentId})/Link
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `parentId` | String | Yes | Parent folder ID |

**Request Body:**

```json
{
  "Name": "Link Name",
  "Description": "Optional description",
  "Uri": "https://example.com"
}
```

**Response:** `Link`

---

### 3.25 Create SymbolicLink

```
POST /sf/v3/Items({accountId})/SymbolicLink
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `accountId` | String | Yes | Account ID |
| `overwrite` | Boolean | No | Overwrite existing (default: false) |

**Request Body:**

```json
{
  "Name": "Connector Name",
  "Description": "Optional description",
  "Zone": { "Id": "zone-id" },
  "ConnectorGroup": { "Id": "group-id" },
  "Link": "optional-link"
}
```

**Response:** `SymbolicLink`

---

### 3.26 Create SymbolicLink via ConnectorGroups

```
POST /sf/v3/ConnectorGroups({id})/Children
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Connector group ID |
| `overwrite` | Boolean | No | Overwrite existing |

**Request Body:** Same as SymbolicLink creation above.

**Response:** `SymbolicLink`

---

### 3.27 Bulk Download

```
POST /sf/v3/Items({parentId})/BulkDownload
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `parentId` | String | Yes | Parent item ID |
| `redirect` | Boolean | No | Redirect to download (default: true) |

**Request Body:** Array of item IDs

```json
["item-id-1", "item-id-2", "item-id-3"]
```

**Response:** 302 redirect or `DownloadSpecification`

---

### 3.28 Copy Item

```
POST /sf/v3/Items({id})/Copy
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Source item ID |
| `targetid` | String | Yes | Target folder ID |
| `overwrite` | Boolean | No | Overwrite existing (default: false) |

**Response:** `Item` if same-zone copy; `AsyncOperation` if the target folder is in a different zone.

> **Async trigger:** Cross-zone copy. Poll the returned `AsyncOperation.Id` via `GET /AsyncOperations({id})` until complete.

---

### 3.29 Check Out (Lock File)

```
POST /sf/v3/Items({id})/CheckOut
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | File ID |

**Response:** Locked `Item`

---

### 3.30 Check In (Unlock File)

```
POST /sf/v3/Items({id})/CheckIn
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | File ID |
| `message` | String | No | Check-in message |

**Response:** Unlocked `Item`

---

### 3.31 Discard Check Out

```
POST /sf/v3/Items({id})/DiscardCheckOut
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | File ID |

**Response:** `Item`

---

### 3.32 Advanced Simple Search

```
POST /sf/v3/Items/AdvancedSimpleSearch
```

**Request Body:**

```json
{
  "Query": {
    "ItemType": "string",
    "ParentID": "string",
    "CreatorID": "string",
    "SearchQuery": "string",
    "CreateStartDate": "2026-01-01",
    "CreateEndDate": "2026-12-31",
    "ItemNameOnly": false
  },
  "Paging": {
    "Count": 50,
    "Skip": 0
  },
  "Sort": {
    "SortBy": "Name",
    "Ascending": true
  },
  "TimeoutInSeconds": 30
}
```

**Response:** `AdvancedSearchResults`

---

### 3.33 Advanced Search

```
POST /sf/v3/Items/AdvancedSearch
```

**Request Body:**

```json
{
  "Query": {
    "ItemTypes": ["File", "Folder"],
    "ParentID": ["folder-id"],
    "CreatorID": ["user-id"],
    "SearchQuery": "search terms",
    "CreateStartDate": "2026-01-01",
    "CreateEndDate": "2026-12-31",
    "ItemNameOnly": false
  },
  "Paging": {
    "Count": 50,
    "Skip": 0
  },
  "Sort": {
    "SortBy": "CreationDate",
    "Ascending": false
  },
  "TimeoutInSeconds": 30
}
```

**Response:** `AdvancedSearchResults`

---

### 3.34 Bulk Delete

```
POST /sf/v3/Items({parentId})/BulkDelete
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `parentId` | String | Yes | Parent item ID |
| `forceSync` | Boolean | No | Block async (default: false) |
| `deletePermanently` | Boolean | No | Permanently delete (default: false) |

**Request Body:** Array of item IDs

```json
["item-id-1", "item-id-2"]
```

---

### 3.35 Bulk Restore

```
POST /sf/v3/Items/BulkRestore
```

**Request Body:**

```json
{
  "ids": ["item-id-1", "item-id-2"]
}
```

---

### 3.36 Bulk Delete Permanently

```
POST /sf/v3/Items/BulkDeletePermanently
```

**Request Body:**

```json
{
  "ids": ["item-id-1", "item-id-2"]
}
```

---

### 3.37 Create WebApp Link

```
POST /sf/v3/Items({id})/WebAppLink
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |

**Response:** `Redirection` with one-time login URI

---

### 3.38 Remove Template Association

```
POST /sf/v3/Items({id})/RemoveTemplateAssociation
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Folder ID |

---

### 3.39 Check Versioning Violation

```
POST /sf/v3/Items({id})/CheckVersioningViolation
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Folder ID |
| `newMaxVersions` | Int32 | Yes | New max version count |

---

### 3.40 Check If Previewable

```
POST /sf/v3/Items({parentId})/CheckIfPreviewable
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `parentId` | String | Yes | Parent item ID |

**Request Body:**

```json
[
  { "FileName": "document.pdf", "FileSizeBytes": 1024000 }
]
```

---

### 3.41 Upload File (Phase 1: Get Upload Specification)

```
POST /sf/v3/Items({folderId})/Upload2
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `folderId` | String | Yes | Target folder ID |

**Request Body:**

```json
{
  "Method": "standard",
  "Raw": false,
  "FileName": "document.pdf",
  "FileSize": 1024000
}
```

**Alternative: Query Parameter Style**

```
POST /sf/v3/Items({folderId})/Upload
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `method` | UploadMethod | No | `standard`, `streamed`, or `threaded` |
| `raw` | Boolean | No | Raw POST body (default: false) |
| `fileName` | String | No | File name |
| `fileSize` | Int64 | No | File size in bytes |
| `batchId` | String | No | Batch identifier |
| `batchLast` | Boolean | No | Last item in batch |
| `canResume` | Boolean | No | Resume support |
| `startOver` | Boolean | No | Restart upload |
| `tool` | String | No | Uploader tool ID (default: `apiv3`) |
| `overwrite` | Boolean | No | Overwrite existing |
| `title` | String | No | Item title |
| `details` | String | No | Item description |
| `isSend` | Boolean | No | Part of Send operation |
| `sendGuid` | String | No | Send operation ID |
| `opid` | String | No | Async operation ID |
| `threadCount` | Int32 | No | Thread count for threaded upload |
| `responseFormat` | String | No | Response format (default: json) |
| `notify` | Boolean | No | Notify users |
| `clientCreatedDateUTC` | DateTime | No | Client filesystem created date |
| `clientModifiedDateUTC` | DateTime | No | Client filesystem modified date |
| `expirationDays` | Int32 | No | Days until expiration |
| `baseFileId` | String | No | Base file ID for conflict checking |

**Response:** `UploadSpecification` containing `ChunkUri`, `IsResume`, `ResumeIndex`, etc.

**Phase 2:** POST the file bytes to the `ChunkUri` returned in the specification.

---

### 3.42 Update Item

```
PATCH /sf/v3/Items({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |
| `overwrite` | Boolean | No | Overwrite existing (default: false) |
| `batchid` | String | No | Batch ID |
| `batchSizeInBytes` | Int64 | No | Batch size |
| `forceSync` | Boolean | No | Execute synchronously |
| `notify` | Boolean | No | Send upload notifications |

**Request Body:**

```json
{
  "Name": "New Name",
  "FileName": "new-filename.pdf",
  "Description": "Updated description",
  "ExpirationDate": "2027-01-01",
  "Parent": { "Id": "new-parent-id" },
  "Zone": { "Id": "zone-id" }
}
```

**Response:** `Item` if zone is unchanged; `AsyncOperation` if the Item's Zone or Parent Zone is modified (cross-zone move).

> **Async trigger:** Changing the item's Zone or moving to a parent in a different Zone. Poll the returned `AsyncOperation.Id` via `GET /AsyncOperations({id})` until complete.

---

### 3.43 Update Link

```
PATCH /sf/v3/Items/Link({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Link ID |
| `notify` | Boolean | No | Notify folder listeners (default: false) |

**Request Body:**

```json
{
  "Name": "Updated Link",
  "Uri": "https://new-url.com",
  "Description": "Updated description",
  "Parent": { "Id": "parent-id" }
}
```

**Response:** Modified `Link`

---

### 3.44 Update Note

```
PATCH /sf/v3/Items/Note({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Note ID |
| `notify` | Boolean | No | Notify folder listeners (default: false) |

**Request Body:**

```json
{
  "Name": "Updated Note",
  "Description": "Updated content",
  "Parent": { "Id": "parent-id" }
}
```

**Response:** Modified `Note`

---

### 3.45 Update SymbolicLink

```
PATCH /sf/v3/Items/SymbolicLink({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | SymbolicLink ID |

**Request Body:**

```json
{
  "Name": "Updated Name",
  "Description": "Updated description",
  "Link": "new-link"
}
```

**Response:** Modified `SymbolicLink`

---

### 3.46 Delete Item

```
DELETE /sf/v3/Items({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |
| `singleversion` | Boolean | No | Delete only specified version (default: false) |
| `forceSync` | Boolean | No | Block async (default: false) |

---

## 4. Users

### 4.1 Get Current User

```
GET /sf/v3/Users
```

**Response:** `User`

---

### 4.2 Get User by ID

```
GET /sf/v3/Users({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | User ID |

**Response:** `User`

---

### 4.3 Get User by Email

```
GET /sf/v3/Users?emailaddress={email}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `emailaddress` | String | Yes | User email address |

**Response:** `User`

---

### 4.4 Create Client User

```
POST /sf/v3/Users
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `pushCreatorDefaultSettings` | Boolean | No | Push creator defaults |
| `addshared` | Boolean | No | Add shared folder access |
| `notify` | Boolean | No | Send notification email |
| `ifNecessary` | Boolean | No | Only create if necessary |
| `addPersonal` | Boolean | No | Add personal folder |

**Request Body:**

```json
{
  "Email": "client@example.com",
  "FirstName": "John",
  "LastName": "Doe",
  "Company": "Acme Corp",
  "DefaultZone": { "Id": "zone-id" }
}
```

**Response:** `User`

---

### 4.5 Create Client User for Folder

```
POST /sf/v3/UsersForFolder
```

Same parameters as Create Client User, plus:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `folderId` | String | Yes | Specific folder ID |

---

### 4.6 Create Employee

```
POST /sf/v3/Users/AccountUser
```

Same query parameters as Create Client User.

**Request Body:**

```json
{
  "Email": "employee@company.com",
  "FirstName": "Jane",
  "LastName": "Smith",
  "Company": "Company Name",
  "StorageQuotaLimitGB": 10,
  "DefaultZone": { "Id": "zone-id" },
  "IsAdministrator": false,
  "CanCreateFolders": true,
  "CanUseFileBox": true,
  "CanManageUsers": false,
  "Roles": ["CanChangePassword", "AdminAccountPolicies"]
}
```

**Response:** `AccountUser`

---

### 4.7 Update User

```
PATCH /sf/v3/Users({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | User ID |

**Request Body:**

```json
{
  "FirstName": "Updated",
  "LastName": "Name",
  "Company": "New Company",
  "Email": "newemail@example.com",
  "Security": { "IsDisabled": false },
  "DefaultZone": { "Id": "zone-id" }
}
```

**Response:** Modified `User`

---

### 4.8 Update Employee

```
PATCH /sf/v3/Users/AccountUser({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | User ID |

**Request Body:**

```json
{
  "FirstName": "Jane",
  "LastName": "Smith",
  "Company": "Company",
  "StorageQuotaLimitGB": 20,
  "Bandwidth": 5000,
  "Security": { "IsDisabled": false },
  "DefaultZone": { "Id": "zone-id" }
}
```

**Response:** Modified `AccountUser`

---

### 4.9 Add Roles

```
PATCH /sf/v3/Users({id})/Roles
```

**Request Body:**

```json
{
  "Roles": ["AdminAccountPolicies", "CanManageUsers"]
}
```

**Response:** Modified `User`

---

### 4.10 Set Roles (Replace All)

```
PUT /sf/v3/Users({id})/Roles
```

**Request Body:**

```json
{
  "Roles": ["CanChangePassword"]
}
```

**Response:** Modified `User`

---

### 4.11 Remove Roles

```
POST /sf/v3/Users({id})/RemoveRoles
```

**Request Body:** `UserRole[]` — roles to remove.

**Response:** Modified `User`

---

### 4.12 Get Home Folder

```
GET /sf/v3/Users({id})/HomeFolder
GET /sf/v3/Users/HomeFolder
```

**Response:** `Folder`

---

### 4.13 Get Top Folders

```
GET /sf/v3/Users({id})/TopFolders
GET /sf/v3/Users/TopFolders
```

**Response:** Feed of `Folder`

---

### 4.14 Get FileBox Children

```
GET /sf/v3/Users({id})/Box
```

**Response:** Feed of `Item`

---

### 4.15 Get FileBox Folder

```
GET /sf/v3/Users({id})/FileBox
```

**Response:** `Folder`

---

### 4.16 Get User Preferences

```
GET /sf/v3/Users({id})/Preferences
GET /sf/v3/Users/Preferences
```

**Response:** `UserPreferences`

---

### 4.17 Update User Preferences

```
PATCH /sf/v3/Users({id})/Preferences
```

**Request Body:**

```json
{
  "EnableFlashUpload": false,
  "EnableJavaUpload": false
}
```

---

### 4.18 Get User Security

```
GET /sf/v3/Users({id})/Security
GET /sf/v3/Users/Security
```

**Response:** `UserSecurity`

---

### 4.19 Reset Password

```
POST /sf/v3/Users/ResetPassword
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `notify` | Boolean | No | Send notification (default: false) |

**Request Body:**

```json
{
  "NewPassword": "newSecurePassword123",
  "OldPassword": "currentPassword"
}
```

---

### 4.20 Forgot Password

```
POST /sf/v3/Users/ForgotPassword
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | String | Yes | User email |
| `resetOnMobile` | Boolean | No | Mobile reset |
| `initiatedByAdmin` | Boolean | No | Admin-initiated reset |

---

### 4.21 Send Welcome Notification

```
POST /sf/v3/Users({id})/WelcomeNotification
```

**Request Body:**

```json
{
  "CustomMessage": "Welcome to our portal!",
  "NotifySender": true
}
```

---

### 4.22 Delete User

```
DELETE /sf/v3/Users({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | User ID |
| `completely` | Boolean | No | Complete removal |
| `itemsReassignTo` | String | No | Reassign items to this user ID |
| `groupsReassignTo` | String | No | Reassign groups to this user ID |

---

### 4.23 Bulk Delete Client Users

```
DELETE /sf/v3/Users/Clients
```

**Request Body:** Array of user IDs

```json
["user-id-1", "user-id-2"]
```

---

### 4.24 Bulk Delete Client Users (POST)

```
POST /sf/v3/Users/Clients/BulkDelete
```

**Request Body:**

```json
{
  "UserIds": ["user-id-1", "user-id-2"]
}
```

---

### 4.25 Downgrade Employees to Clients

```
POST /sf/v3/Users/Employees/Downgrade
```

**Request Body:**

```json
{
  "UserIds": ["user-id-1", "user-id-2"],
  "ReassignItemsToId": "admin-user-id",
  "ReassignGroupsToId": "admin-user-id"
}
```

---

### 4.26 Get Shared Folders

```
GET /sf/v3/Users/AllSharedFolders
GET /sf/v3/Users({id})/AllSharedFolders
```

**Response:** Feed of `Folder`

---

### 4.27 Get Network Share Connectors

```
GET /sf/v3/Users/NetworkShareConnectors
```

**Response:** Feed of `Folder`

---

### 4.28 Get SharePoint Connectors

```
GET /sf/v3/Users/SharepointConnectors
```

**Response:** Feed of `Folder`

---

### 4.29 Confirm First-Time Login

```
POST /sf/v3/Users/Confirm
```

**Request Body:**

```json
{
  "FirstName": "John",
  "LastName": "Doe",
  "Company": "Company",
  "Password": "securePassword",
  "SecurityQuestion": "What is your pet's name?",
  "SecurityQuestionAnswer": "Rex",
  "DayLightName": "Eastern Standard Time",
  "UTCOffset": "-05:00",
  "DateFormat": "MM/dd/yyyy",
  "TimeFormat": "h:mm tt",
  "EmailInterval": 0,
  "UserNotificationLocale": "en-US"
}
```

---

### 4.30 Get User Info

```
GET /sf/v3/Users/Info
```

**Response:** `UserInfo`

---

### 4.31 Delete Email Address

```
POST /sf/v3/Users/DeleteEmailAddress?email={email}
```

**Response:** `User`

---

### 4.32 Set Primary Email

```
POST /sf/v3/Users/MakePrimary?email={email}
```

**Response:** `User`

---

### 4.33 Send Confirmation Email

```
POST /sf/v3/Users/SendConfirmationEmail?email={email}
```

**Response:** `User`

---

### 4.34 Send Email Verification Code

```
POST /sf/v3/Users/SendEmailVerificationCode?email={email}
```

**Response:** 204 No Content

---

### 4.35 Verify Email Address

```
POST /sf/v3/Users/VerifyEmailAddress?email={email}&verificationCode={code}
```

**Response:** 204 No Content

---

### 4.36 Create WebApp Login Link

```
POST /sf/v3/Users/WebAppLink
```

**Response:** `Redirection` with one-time login URI

---

### 4.37 Get Inbox Metadata

```
GET /sf/v3/Users/InboxMetadata
GET /sf/v3/Users({id})/InboxMetadata
```

**Response:** Inbox metadata

---

### 4.38 Get Inbox

```
GET /sf/v3/Users/Inbox
GET /sf/v3/Users({id})/Inbox
```

**Response:** Feed of `Share`

---

### 4.39 Get Sent Messages

```
GET /sf/v3/Users/SentMessages
GET /sf/v3/Users({id})/SentMessages
```

**Response:** Feed of `Share`

---

### 4.40 Get User Groups

```
GET /sf/v3/Users({id})/Groups
```

**Response:** Feed of `Group`

---

### 4.41 Create Manage User Link

```
POST /sf/v3/Users({id})/WebAppManageUser
```

**Response:** `Redirection`

---

### 4.42 Create Manage Users Link

```
POST /sf/v3/Users/WebAppManageUsers
```

**Response:** `Redirection`

---

### 4.43 Create Add Employee Link

```
POST /sf/v3/Users/WebAppAddEmployee
```

**Response:** `Redirection`

---

## 5. Shares

Shares provide temporary access to files or folders for downloading or uploading.

### 5.1 Get List of Shares

```
GET /sf/v3/Shares
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `includeExpired` | Boolean | No | Include expired shares (default: true) |

**Response:** Feed of `Share`

---

### 5.2 Get Share by ID

```
GET /sf/v3/Shares({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Share ID |
| `includeExpired` | Boolean | No | Include expired (default: false) |

**Response:** `Share`

---

### 5.3 Get Share Recipients

```
GET /sf/v3/Shares({id})/Recipients
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Share ID |
| `includeExpired` | Boolean | No | Include expired (default: false) |

**Response:** Feed of `ShareAlias`

---

### 5.4 Get Single Recipient

```
GET /sf/v3/Shares({shareId})/Recipients({recipientId})
```

**Response:** `ShareAlias`

---

### 5.5 Create Recipient

```
POST /sf/v3/Shares({shareId})/Recipients
```

**Request Body:**

```json
{
  "Email": "recipient@example.com",
  "FirstName": "John",
  "LastName": "Doe",
  "Company": "Acme"
}
```

**Response:** `ShareAlias`

---

### 5.6 Get Share Items

```
GET /sf/v3/Shares({id})/Items
```

**Response:** Feed of `Item`

---

### 5.7 Get Single Share Item

```
GET /sf/v3/Shares({shareId})/Items({itemId})
```

**Response:** `Item`

---

### 5.8 Get Share Item Thumbnail

```
GET /sf/v3/Shares({shareId})/Items({itemId})/Thumbnail
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `size` | Int32 | No | 75 (default) or 600 |
| `redirect` | Boolean | No | 302 redirect or Redirection object |

---

### 5.9 Get Share Item Protocol Links

```
GET /sf/v3/Shares({shareId})/Items({itemId})/ProtocolLinks({platform})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `platform` | String | Yes | `all`, `default`, `web`, `webandmobile`, `mobile` |

---

### 5.10 Download Share Items

```
GET /sf/v3/Shares({shareId})/Download
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `shareId` | String | Yes | Share ID |
| `Name` | String | No | Recipient name |
| `Email` | String | No | Recipient email |
| `Company` | String | No | Recipient company |
| `id` | String | No | Specific item ID to download |
| `redirect` | Boolean | No | Redirect (default: true) |

**Response:** 302 redirect or `DownloadSpecification`

---

### 5.11 Download for Recipient

```
GET /sf/v3/Shares({shareId})/Recipients({aliasId})/DownloadWithAlias
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | No | Specific item ID |
| `redirect` | Boolean | No | Redirect (default: true) |

---

### 5.12 Bulk Download for Recipient

```
POST /sf/v3/Shares({shareId})/Recipients({aliasId})/BulkDownload
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `redirect` | Boolean | No | Redirect (default: true) |
| `includeExpired` | Boolean | No | Include expired (default: false) |

**Request Body:** Array of item IDs

---

### 5.13 Create Share

```
POST /sf/v3/Shares
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `notify` | Boolean | No | Send notification (default: false) |
| `direct` | Boolean | No | Direct share (default: false) |

**Request Body:**

```json
{
  "ShareType": "Send",
  "Title": "Shared Documents",
  "Items": [
    { "Id": "item-id-1" },
    { "Id": "item-id-2" }
  ],
  "Recipients": [
    { "User": { "Email": "user@example.com" } }
  ],
  "ExpirationDate": "2026-06-30",
  "RequireLogin": false,
  "RequireUserInfo": false,
  "IsViewOnly": false
}
```

For **Request** shares (file requests), also include:

```json
{
  "ShareType": "Request",
  "Parent": { "Id": "folder-id" },
  "TrackUntilDate": "2026-07-31",
  "SendFrequency": 7,
  "SendInterval": 1
}
```

**Response:** `Share`

---

### 5.14 Update Share

```
PATCH /sf/v3/Shares({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Share ID |
| `appendItemsFeed` | Boolean | No | Append items (default: true) |
| `includeExpired` | Boolean | No | Include expired (default: false) |

**Request Body:** Partial `Share` object.

**Response:** Modified `Share`

---

### 5.15 Delete Share

```
DELETE /sf/v3/Shares({id})
```

---

### 5.16 Create Share Alias

```
POST /sf/v3/Shares({id})/Alias
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | String | Yes | Recipient email |
| `notify` | Boolean | No | Send notification |

**Response:** `Share` with `AliasID` property

---

### 5.17 Delete Share Alias

```
DELETE /sf/v3/Shares({id})/Alias?email={email}
```

---

### 5.18 Send File Email

```
POST /sf/v3/Shares/Send
```

**Request Body:**

```json
{
  "Items": ["item-id-1", "item-id-2"],
  "Emails": ["user1@example.com", "user2@example.com"],
  "Subject": "Files for your review",
  "Body": "Please review these documents.",
  "CcSender": true,
  "NotifyOnDownload": true,
  "RequireLogin": false,
  "ExpirationDays": 30
}
```

**Response:** `Share`

---

### 5.19 Send Callback

```
POST /sf/v3/Shares/SendCallback
```

**Request Body:**

```json
{
  "RecipientArray": ["email1@example.com"],
  "SfShareId": "share-id",
  "ShareSendParams": { ... }
}
```

---

### 5.20 Request File Email

```
POST /sf/v3/Shares/Request
```

**Request Body:**

```json
{
  "FolderId": "folder-id",
  "Emails": ["uploader@example.com"],
  "Subject": "Please upload documents",
  "Body": "We need these files by Friday.",
  "CcSender": true,
  "NotifyOnUpload": true,
  "RequireLogin": false,
  "ExpirationDays": 30,
  "IsViewOnly": false
}
```

**Response:** `Share`

---

### 5.21 Resend Share Email

```
POST /sf/v3/Shares/Resend
```

**Request Body:**

```json
{
  "ShareId": "share-id",
  "Recipients": ["email@example.com"],
  "Subject": "Reminder: Shared files",
  "Body": "Just a reminder.",
  "CcSender": false,
  "NotifyOnUse": true
}
```

---

### 5.22 Upload to Request Share

```
POST /sf/v3/Shares({id})/Upload2
```

Same parameters as Items Upload (section 3.41).

**Response:** `UploadSpecification`

---

### 5.23 Get Share Redirection

```
GET /sf/v3/Shares({id})/Redirection
```

**Response:** `Redirection`

---

### 5.24 Get Inbox

```
GET /sf/v3/Shares/Inbox
GET /sf/v3/Shares/Inbox({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | String | No | User ID |
| `type` | ShareType | No | Filter by type |
| `archived` | Boolean | No | Include archived |

**Response:** Feed of `Share`

---

### 5.25 Get Sent Message Content

```
GET /sf/v3/Shares({shareId})/Recipients({aliasId})/Message
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `asJson` | Boolean | No | Return as JSON (default: false) |
| `includeExpired` | Boolean | No | Include expired (default: false) |

**Response:** Message string or JSON object

---

## 6. Accounts

### 6.1 Get Current Account

```
GET /sf/v3/Accounts
GET /sf/v3/Accounts({id})
```

**Response:** `Account`

---

### 6.2 Get Branding

```
GET /sf/v3/Accounts/Branding
```

No authentication required.

**Response:** Branding object

---

### 6.3 Update Branding

```
PATCH /sf/v3/Accounts/Branding
```

**Request Body:** `Branding` object

---

### 6.4 Get Employees

```
GET /sf/v3/Accounts/Employees
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `withRightSignature` | Boolean | No | Filter by RightSignature |

**Response:** Feed of `Contact`

---

### 6.5 Get Clients

```
GET /sf/v3/Accounts/Clients
```

**Response:** Feed of `Contact`

---

### 6.6 Get Address Book

```
GET /sf/v3/Accounts/AddressBook
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | String | Yes | `personal`, `shared`, or `group` |
| `searchTerm` | String | No | Search filter |

**Response:** Feed of `Contact`

---

### 6.7 Get Mobile Security Settings

```
GET /sf/v3/Accounts/MobileSecuritySettings
```

**Response:** Mobile security configuration

---

### 6.8 Get Product Defaults

```
GET /sf/v3/Accounts/ProductDefaults
```

**Response:** Account defaults

---

### 6.9 Get Preferences

```
GET /sf/v3/Accounts/Preferences
```

**Response:** `AccountPreferences`

---

### 6.10 Update Preferences

```
PATCH /sf/v3/Accounts/Preferences
```

Requires `AdminAccountPolicies` role.

**Request Body:**

```json
{
  "EnableDLP": true,
  "PasswordPolicy": {
    "MaxAgeDays": 90,
    "HistoryCount": 5,
    "MinimumLength": 12,
    "MinimumNumeric": 1,
    "MinimumSpecialCharacters": 1
  }
}
```

---

### 6.11 Get SSO Configuration

```
GET /sf/v3/Accounts/SSO
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `provider` | String | No | Reserved |
| `idpEntityId` | String | No | IDP Entity ID |

**Response:** `SSOAccountProvider`

---

### 6.12 Update SSO Configuration

```
PATCH /sf/v3/Accounts/SSO
```

**Request Body:**

```json
{
  "LogoutUrl": "https://idp.example.com/logout",
  "LoginUrl": "https://idp.example.com/login",
  "IPRestrictions": "10.0.0.0/8",
  "ForceSSO": true,
  "EntityID": "https://idp.example.com/entity",
  "SFEntityID": "https://mycompany.sharefile.com",
  "SPInitatedAuthContext": "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport",
  "SPInitatedAuthMethod": "POST"
}
```

---

### 6.13 Email Account List

```
POST /sf/v3/Accounts/SendToEmail?email={email}
```

No authentication required.

---

### 6.14 Get Login Access Control Domains

```
GET /sf/v3/Accounts/LoginAccessControlDomains
```

**Response:** `AccessControlDomains`

---

### 6.15 Get Folder Access Control Domains

```
GET /sf/v3/Accounts/FolderAccessControlDomains
```

**Response:** `AccessControlDomains`

---

### 6.16 Create Login Access Control Domains

```
POST /sf/v3/Accounts/LoginAccessControlDomains
```

**Request Body:**

```json
{
  "AccessControlType": "AllowedDomains",
  "Domains": ["company.com", "partner.com"]
}
```

---

### 6.17 Create Folder Access Control Domains

```
POST /sf/v3/Accounts/FolderAccessControlDomains
```

Same body format as above.

---

### 6.18 Update Login Access Control Domains

```
PATCH /sf/v3/Accounts/LoginAccessControlDomains
```

Merges with existing domains.

---

### 6.19 Update Folder Access Control Domains

```
PATCH /sf/v3/Accounts/FolderAccessControlDomains
```

Merges with existing domains.

---

### 6.20 Delete Login Access Control Domains

```
DELETE /sf/v3/Accounts/LoginAccessControlDomains
```

**Request Body:** Domains to remove.

---

### 6.21 Delete Folder Access Control Domains

```
DELETE /sf/v3/Accounts/FolderAccessControlDomains
```

**Request Body:** Domains to remove.

---

### 6.22 Check WebPop Requirement

```
GET /sf/v3/Accounts/RequireWebPop
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `subdomain` | String | Yes | Account subdomain |
| `username` | String | Yes | Email or user ID |
| `singlePlane` | Boolean | No | Single plane mode (default: false) |

**Response:** `RequireWebPopResult`

---

### 6.23 Check Subdomain Requirement

```
GET /sf/v3/Accounts/RequireSubdomain
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `username` | String | Yes | User email |
| `singlePlane` | Boolean | No | Single plane mode |

**Response:** `RequireSubdomainResult`

---

### 6.24 Find Subdomain

```
POST /sf/v3/Accounts/FindSubdomain
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `singlePlane` | Boolean | No | Single plane mode |

**Request Body:**

```json
{
  "UsernameShort": "user@example.com",
  "Password": "password",
  "EmployeeOnly": false
}
```

**Response:** `FindSubdomainResult`

---

### 6.25 Get Outlook Information

```
GET /sf/v3/Accounts/OutlookInformation
```

**Response:** `OutlookInformation`

---

### 6.26 Get SSO Info

```
GET /sf/v3/Accounts/SSOInfo?subdomain={subdomain}
```

**Response:** `SSOInfo`

---

### 6.27 Get Partner Tenants

```
GET /sf/v3/Accounts/Tenants
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `multiPlane` | Boolean | No | Multi-plane mode (default: false) |
| `partnerAccountId` | String | No | Partner account ID |

**Response:** Feed of tenant `Account`

---

### 6.28 Get Tenant by ID

```
GET /sf/v3/Accounts/Tenants({id})
```

Supports `$expand=DiskSpace`.

**Response:** Tenant `Account`

---

### 6.29 Get Tenant Zone Usage

```
GET /sf/v3/Accounts/Tenants/ZoneUsage
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `includePartner` | Boolean | No | Include partner usage |

---

### 6.30 Get Tenant Zones

```
GET /sf/v3/Accounts/Tenants({id})/Zones
```

**Response:** Feed of `Zone`

---

### 6.31 Create Admin WebApp Link

```
POST /sf/v3/Accounts/WebAppAdmin
```

**Response:** `Redirection` with one-time admin login URI

---

## 7. Groups

### 7.1 Get All Groups

```
GET /sf/v3/Groups
```

**Response:** Feed of `Group`

---

### 7.2 Get Group by ID

```
GET /sf/v3/Groups({id})
```

**Response:** `Group`

---

### 7.3 Create Group

```
POST /sf/v3/Groups
```

**Request Body:**

```json
{
  "Name": "Engineering Team",
  "IsShared": true,
  "Contacts": [
    { "Email": "user1@example.com" },
    { "Email": "user2@example.com" }
  ]
}
```

**Response:** `Group`

---

### 7.4 Update Group

```
PATCH /sf/v3/Groups({id})
```

**Request Body:** Partial `Group` object.

**Response:** Modified `Group`

---

### 7.5 Delete Group

```
DELETE /sf/v3/Groups({id})
```

---

### 7.6 Get Group Contacts

```
GET /sf/v3/Groups({id})/Contacts
```

**Response:** Feed of `Contact`

---

### 7.7 Add Contacts to Group

```
POST /sf/v3/Groups({id})/Contacts
```

**Request Body:**

```json
[
  { "Email": "newuser@example.com" },
  { "Id": "existing-user-id" }
]
```

**Response:** Updated contacts list

---

### 7.8 Remove Contacts from Group

```
DELETE /sf/v3/Groups({id})/Contacts
```

**Request Body:**

```json
[
  { "Email": "remove@example.com" },
  { "Id": "user-id-to-remove" }
]
```

---

### 7.9 Export Group Contacts

```
GET /sf/v3/Groups({id})/ExportDocument
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Group ID |
| `documentType` | DocumentType | No | Export format |
| `token` | String | No | Auth token |

**Response:** Spreadsheet or CSV file

---

### 7.10 Get Groups for User

```
GET /sf/v3/Users({id})/Groups
```

**Response:** Feed of `Group`

---

## 8. AccessControls

### 8.1 Get AccessControl by ID

```
GET /sf/v3/AccessControls(principalid={principalId},itemid={itemId})
```

**Response:** `AccessControl`

---

### 8.2 Get AccessControls by Item

```
GET /sf/v3/Items({id})/AccessControls
```

**Response:** Feed of `AccessControl`

---

### 8.3 Create AccessControl

```
POST /sf/v3/Items({id})/AccessControls
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |
| `recursive` | Boolean | No | Apply recursively (default: false) |
| `message` | String | No | Notification message |
| `sendDefaultNotification` | Boolean | No | Send notification (default: false) |

**Request Body:**

```json
{
  "Principal": { "Id": "user-or-group-id" },
  "CanUpload": true,
  "CanDownload": true,
  "CanView": true,
  "CanDelete": false,
  "CanManagePermissions": false
}
```

**Response:** `AccessControl` if synchronous; `AsyncOperation` if fulfilled asynchronously (typically when `recursive=true`).

> **Async trigger:** Recursive permission application across a folder tree. Poll the returned `AsyncOperation.Id` via `GET /AsyncOperations({id})` until complete.

---

### 8.4 Update AccessControl

```
PATCH /sf/v3/Items({id})/AccessControls
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Item ID |
| `recursive` | Boolean | No | Apply recursively (default: false) |

**Request Body:** Same as create.

**Response:** `AccessControl` if synchronous; `AsyncOperation` if fulfilled asynchronously (typically when `recursive=true`).

> **Async trigger:** Recursive permission update across a folder tree. Poll the returned `AsyncOperation.Id` via `GET /AsyncOperations({id})` until complete.

---

### 8.5 Delete AccessControl

```
DELETE /sf/v3/AccessControls(principalid={principalId},itemid={itemId})
```

**Response:** 204 No Content

---

### 8.6 Bulk Set AccessControls

```
POST /sf/v3/Items({id})/AccessControls/BulkSet
```

**Request Body:**

```json
{
  "NotifyUser": true,
  "NotifyMessage": "You've been granted access",
  "AccessControlParams": [
    {
      "AccessControl": {
        "Principal": { "Id": "user-id" },
        "CanUpload": true,
        "CanDownload": true,
        "CanView": true,
        "CanDelete": false,
        "CanManagePermissions": false
      },
      "NotifyUser": true,
      "Recursive": false
    }
  ]
}
```

**Response:** `AccessControlBulkResult`

---

### 8.7 Bulk Set for Principal

```
POST /sf/v3/AccessControls/BulkSetForPrincipal?principalId={principalId}
```

**Request Body:** Same structure as BulkSet.

**Response:** `AccessControlBulkResult`

---

### 8.8 Clone AccessControls

```
POST /sf/v3/AccessControls/Clone
```

**Request Body:**

```json
{
  "FolderId": "folder-id",
  "PrincipalId": "source-principal-id",
  "ClonePrincipalIds": ["target-id-1", "target-id-2"]
}
```

---

### 8.9 Bulk Delete AccessControls

```
POST /sf/v3/Items({id})/AccessControls/BulkDelete
```

**Request Body:** Array of principal IDs

```json
["principal-id-1", "principal-id-2"]
```

---

### 8.10 Bulk Delete for Principal

```
POST /sf/v3/AccessControls/BulkDeleteForPrincipal?principalId={principalId}
```

**Request Body:** Array of folder IDs

```json
["folder-id-1", "folder-id-2"]
```

---

### 8.11 Notify Users of Access

```
POST /sf/v3/Items({id})/AccessControls/NotifyUsers
```

**Request Body:**

```json
{
  "UserIds": ["user-id-1", "user-id-2"],
  "CustomMessage": "You now have access to this folder."
}
```

---

### 8.12 Preview Notification Email

```
POST /sf/v3/Items({id})/AccessControls/NotifyUsersPreview
```

**Request Body:** `NotifyUsersParams`

---

## 9. Zones

### 9.1 Get All Zones

```
GET /sf/v3/Zones
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `services` | ZoneService | No | Filter by service type |
| `includeDisabled` | Boolean | No | Include disabled zones |

**Response:** Feed of `Zone`

---

### 9.2 Get Zone by ID

```
GET /sf/v3/Zones({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | Zone ID |
| `secret` | Boolean | No | Include secret |

**Response:** `Zone`

---

### 9.3 Create Zone

```
POST /sf/v3/Zones
```

**Request Body:**

```json
{
  "Name": "On-Premise Zone",
  "HeartbeatTolerance": 300,
  "ZoneServices": "StorageZone"
}
```

**Response:** `Zone`

---

### 9.4 Update Zone

```
PATCH /sf/v3/Zones({id})
```

**Request Body:**

```json
{
  "Name": "Updated Zone Name",
  "HeartbeatTolerance": 600,
  "ZoneServices": "StorageZone"
}
```

**Response:** Modified `Zone`

---

### 9.5 Delete Zone

```
DELETE /sf/v3/Zones({id})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `force` | Boolean | No | Force deletion |
| `newDefaultZoneId` | String | No | New default zone |

---

### 9.6 Reset Zone Secret

```
POST /sf/v3/Zones({id})/ResetSecret
```

**Response:** Modified `Zone`

---

### 9.7 Get Multi-Tenant Zone Tenants

```
GET /sf/v3/Zones({id})/Tenants
```

**Response:** Feed of tenant accounts

---

### 9.8 Add Tenant to Zone

```
POST /sf/v3/Zones({id})/Tenants?accountId={accountId}
```

---

### 9.9 Remove Tenant from Zone

```
DELETE /sf/v3/Zones({id})/Tenants({tenantId})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `newDefaultZoneId` | String | No | New default zone for tenant |
| `expireItems` | Boolean | No | Expire tenant items |

---

### 9.10 Get Zone Metadata

```
GET /sf/v3/Zones({id})/Metadata
```

**Response:** Feed of `Metadata`

---

### 9.11 Create/Update Zone Metadata

```
POST /sf/v3/Zones({id})/Metadata
```

**Request Body:**

```json
[
  {
    "Name": "custom-key",
    "Value": "custom-value",
    "IsPublic": true
  }
]
```

---

### 9.12 Delete Zone Metadata

```
DELETE /sf/v3/Zones({id})/Metadata?name={metadataName}
```

---

## 10. Devices

### 10.1 Get Current User Devices

```
GET /sf/v3/Devices
```

**Response:** Feed of `DeviceUser`

---

### 10.2 Get Device by ID

```
GET /sf/v3/Devices({id})
```

**Response:** `Device`

---

### 10.3 Get Devices for User

```
GET /sf/v3/User({userId})/Devices
```

**Response:** Feed of `Device`

---

### 10.4 Delete Device for User

```
DELETE /sf/v3/User({userId})/Devices({deviceId})
```

---

### 10.5 Wipe Device

```
POST /sf/v3/Devices({deviceId})/Wipe
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userid` | String | No | User ID or email |

---

### 10.6 Lock Device

```
POST /sf/v3/Devices({deviceId})/Lock
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userid` | String | No | User ID or email |

---

### 10.7 Unlock Device

```
POST /sf/v3/Devices({deviceId})/Unlock
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userid` | String | No | User ID or email |

---

## 11. WebhookSubscriptions

### 11.1 Get All Subscriptions

```
GET /sf/v3/WebhookSubscriptions
```

**Response:** Feed of `WebhookSubscription`

---

### 11.2 Get Subscription by ID

```
GET /sf/v3/WebhookSubscriptions({id})
```

**Response:** `WebhookSubscription`

---

### 11.3 Create Subscription

```
POST /sf/v3/WebhookSubscriptions
```

**Request Body:**

```json
{
  "SubscriptionContext": {
    "ResourceType": "Items",
    "ResourceId": "folder-id"
  },
  "WebhookUrl": "https://myapp.example.com/webhooks/sharefile",
  "Events": [
    {
      "ResourceType": "Items",
      "OperationName": "Upload"
    },
    {
      "ResourceType": "Items",
      "OperationName": "Delete"
    }
  ]
}
```

**Response:** `WebhookSubscription`

---

### 11.4 Delete Subscription

```
DELETE /sf/v3/WebhookSubscriptions({id})
```

---

## 12. AsyncOperations

### 12.1 Get AsyncOperation by ID

```
GET /sf/v3/AsyncOperations({id})
```

**Response:** `AsyncOperation`

---

### 12.2 Get by Batch ID

```
GET /sf/v3/AsyncOperations/GetByBatch({batchId})
```

**Response:** Feed of `AsyncOperation`

---

### 12.3 Get Batch Progress

```
GET /sf/v3/AsyncOperations/GetBatch({batchId})
```

**Response:** `AsyncOperation` with batch progress

---

### 12.4 Get by Folder

```
GET /sf/v3/AsyncOperations/GetByFolder({folderId})
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `activeOnly` | Boolean | No | Filter out completed operations |

**Response:** Feed of `AsyncOperation`

---

### 12.5 Cancel Operation

```
POST /sf/v3/AsyncOperations({id})/Cancel
```

**Response:** Modified `AsyncOperation`

---

### 12.6 Cancel Batch

```
POST /sf/v3/AsyncOperations/CancelBatch({batchId})
```

**Response:** Feed of modified `AsyncOperation`

---

### 12.7 Update Operation State

```
PATCH /sf/v3/AsyncOperations({id})
```

**Request Body:**

```json
{
  "State": "Completed"
}
```

**Response:** Modified `AsyncOperation`

---

### 12.8 Delete Operation

```
DELETE /sf/v3/AsyncOperations({id})
```

---

## 13. Models

### 13.1 Item

| Property | Type | Description |
|----------|------|-------------|
| `Id` | String | Unique identifier |
| `Name` | String | Item display name |
| `FileName` | String | Download file name |
| `Creator` | User | Creating user |
| `Parent` | Item | Parent container |
| `AccessControls` | List\<AccessControl\> | ACLs on this item |
| `Zone` | Zone | Storage zone |
| `CreationDate` | DateTime | Created timestamp |
| `ProgenyEditDate` | DateTime | Last modified (recursive) |
| `LastModifiedByUserID` | String | Last modifier user ID |
| `ClientCreatedDate` | DateTime | Client filesystem created date |
| `ClientModifiedDate` | DateTime | Client filesystem modified date |
| `ExpirationDate` | DateTime | Auto-deletion date |
| `Description` | String | Item description |
| `DiskSpaceLimit` | Int32 | Max bytes for container |
| `IsHidden` | Boolean | Hidden flag |
| `BandwidthLimitInMB` | Int32 | Bandwidth cap |
| `Owner` | User | Item owner |
| `Account` | Account | Containing account |
| `FileSizeInKB` | Int32 | Size in kilobytes |
| `FileSizeBytes` | Int64 | Size in bytes |
| `Path` | String | Virtual root path |
| `CreatorFirstName` | String | Creator first name |
| `CreatorLastName` | String | Creator last name |
| `ExpirationDays` | Int32 | Days until expiration |
| `PreviewStatus` | PreviewStatus | Preview availability |
| `HasPendingDeletion` | Boolean | Pending removal |
| `AssociatedFolderTemplateID` | String | Template reference |
| `IsTemplateOwned` | Boolean | Created from template |
| `StreamID` | String | Version stream ID |
| `HasMultipleVersions` | Boolean | Has other versions |
| `HasPendingAsyncOp` | Boolean | Pending operation |
| `Metadata` | List\<Metadata\> | Custom metadata |
| `Favorite` | Favorite | Favorite object |
| `SemanticPath` | String | Path using folder names |

### 13.2 Folder (extends Item)

| Property | Type | Description |
|----------|------|-------------|
| `FileCount` | Int32 | Child item count (including sub-folders) |
| `Children` | List\<Item\> | Child items |
| `HasRemoteChildren` | Boolean | Children on remote endpoint |
| `Info` | ItemInfo | Effective access control permissions |
| `Redirection` | Redirection | Redirection endpoint |

### 13.3 User

| Property | Type | Description |
|----------|------|-------------|
| `Id` | String | Unique identifier |
| `Email` | String | Primary email |
| `FirstName` | String | First name |
| `LastName` | String | Last name |
| `Company` | String | Company name |
| `DefaultZone` | Zone | Default storage zone |
| `Security` | UserSecurity | Security settings |
| `Preferences` | UserPreferences | User preferences |
| `Roles` | List\<String\> | Assigned roles |

### 13.4 AccountUser (extends User)

| Property | Type | Description |
|----------|------|-------------|
| `StorageQuotaLimitGB` | Int32 | Storage quota in GB |
| `IsAdministrator` | Boolean | Admin flag |
| `CanCreateFolders` | Boolean | Folder creation permission |
| `CanUseFileBox` | Boolean | FileBox access |
| `CanManageUsers` | Boolean | User management permission |
| `Bandwidth` | Int32 | Bandwidth limit |

### 13.5 Share

| Property | Type | Description |
|----------|------|-------------|
| `Id` | String | Unique identifier |
| `ShareType` | ShareType | `Send` or `Request` |
| `Title` | String | Share title |
| `Items` | List\<Item\> | Shared items |
| `Recipients` | List\<ShareAlias\> | Share recipients |
| `Parent` | Item | Parent folder (Request shares) |
| `ExpirationDate` | DateTime | Expiration date |
| `RequireLogin` | Boolean | Login required to access |
| `RequireUserInfo` | Boolean | User info required |
| `IsViewOnly` | Boolean | View-only restriction |
| `TrackUntilDate` | DateTime | Tracking end date (Request) |
| `SendFrequency` | Int32 | Reminder frequency (Request) |
| `SendInterval` | Int32 | Reminder interval (Request) |
| `AliasID` | String | Share alias ID |

### 13.6 Group

| Property | Type | Description |
|----------|------|-------------|
| `Id` | String | Unique identifier |
| `Name` | String | Group name |
| `IsShared` | Boolean | Shared/distribution group |
| `Contacts` | List\<Contact\> | Group members |

### 13.7 Account

| Property | Type | Description |
|----------|------|-------------|
| `Id` | String | Unique identifier |
| `Subdomain` | String | Account subdomain |
| `Preferences` | AccountPreferences | Account preferences |
| `Branding` | Branding | Branding settings |
| `SSO` | SSOAccountProvider | SSO configuration |

### 13.8 AccessControl

| Property | Type | Description |
|----------|------|-------------|
| `Principal` | User or Group | The user or group |
| `CanUpload` | Boolean | Upload permission |
| `CanDownload` | Boolean | Download permission |
| `CanView` | Boolean | View permission |
| `CanDelete` | Boolean | Delete permission |
| `CanManagePermissions` | Boolean | Permission management |

### 13.9 Zone

| Property | Type | Description |
|----------|------|-------------|
| `Id` | String | Unique identifier |
| `Name` | String | Zone name |
| `HeartbeatTolerance` | Int32 | Heartbeat tolerance in seconds |
| `ZoneServices` | ZoneService | Zone service type |
| `Secret` | String | Zone secret (when requested) |

### 13.10 Device

| Property | Type | Description |
|----------|------|-------------|
| `Id` | String | Unique identifier |
| `DeviceName` | String | Device name |
| `DeviceType` | String | Device type |
| `User` | User | Associated user |

### 13.11 WebhookSubscription

| Property | Type | Description |
|----------|------|-------------|
| `Id` | String | Unique identifier |
| `SubscriptionContext` | SubscriptionContext | Resource type and ID |
| `WebhookUrl` | String | Callback URL |
| `Events` | List\<WebhookEvent\> | Subscribed events |

### 13.12 AsyncOperation

| Property | Type | Description |
|----------|------|-------------|
| `Id` | String | Unique identifier |
| `State` | String | Operation state |
| `BatchId` | String | Batch identifier |
| `Progress` | Int32 | Completion percentage |

### 13.13 Supporting Types

| Type | Properties |
|------|-----------|
| `DownloadSpecification` | `DownloadUrl`, `DownloadToken`, `PrepStatus` |
| `UploadSpecification` | `Method`, `ChunkUri`, `IsResume`, `ResumeIndex`, `ResumeOffset`, `ResumeFileHash` |
| `Redirection` | `Uri`, `Available`, `Expiration` |
| `SearchResults` | `Results` (List\<Item\>), `TotalCount`, `TimedOut` |
| `AdvancedSearchResults` | `Results`, `TotalCount`, `PartialResults`, `TimedOut` |
| `ItemInfo` | `HasVroot`, `IsSystemRoot`, `IsAccountRoot`, `IsVRoot`, `IsMyFolders`, `IsAHomeFolder`, `IsMyHomeFolder`, `IsAStartFolder`, `IsSharedFolder`, `IsPassthrough`, `CanAddFolder`, `CanAddNode`, `CanView`, `CanDownload`, `CanUpload`, `CanSend`, `CanDeleteCurrentItem`, `CanDeleteChildItems`, `CanManagePermissions`, `FolderPayID`, `ShowFolderPayBuyButton` |
| `Contact` | `Id`, `Email`, `FirstName`, `LastName`, `Company` |
| `Metadata` | `Name`, `Value`, `IsPublic` |
| `Favorite` | `Id`, `Item`, `CreationDate` |
| `ShareAlias` | `Id`, `Email`, `FirstName`, `LastName`, `Company`, `DownloadCount` |
| `AccessControlDomains` | `AccessControlType`, `Domains` (List\<String\>) |
| `SSOAccountProvider` | `LogoutUrl`, `LoginUrl`, `IPRestrictions`, `ForceSSO`, `EntityID`, `SFEntityID` |
| `OAuthToken` | `access_token`, `refresh_token`, `token_type`, `apicp`, `appcp`, `subdomain`, `expires_in` |

---

## 14. Enums

| Enum | Values |
|------|--------|
| `ShareType` | `Send`, `Request` |
| `UploadMethod` | `Standard`, `Streamed`, `Threaded` |
| `TreeMode` | `Copy`, `Move`, `Manage` |
| `ItemOrderingMode` | `FoldersFirst`, `DateDesc`, `DateAsc`, `NameAsc`, `NameDesc` |
| `DlpStatus` | `Unscanned`, `ScannedOK`, `ScannedRejected` |
| `PreviewStatus` | `None`, `Available`, `Unavailable` |
| `ZoneService` | `StorageZone`, `SharePoint`, `NetworkShareConnector` |
| `DocumentType` | Spreadsheet, CSV |
| `GrantType` | `authorization_code`, `password`, `refresh_token` |
