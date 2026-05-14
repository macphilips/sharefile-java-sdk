# ShareFile API Endpoint Coverage Audit

**Reference:** `docs/API_REFERENCE.md`
**SDK inspected:** `sharefile-sdk-client/src/main/java/io/github/indraftapp/sharefile/client`
**Audit mode:** Strict endpoint matching

## Source References

- API endpoint source: `docs/API_REFERENCE.md`
- Public SDK entrypoint: `ShareFileClient.java`
- Resource clients inspected: `ItemsClient.java`, `UsersClient.java`, `SharesClient.java`,
  `AccountsClient.java`, `GroupsClient.java`, `AccessControlsClient.java`, `ZonesClient.java`,
  `WebhookSubscriptionsClient.java`, `AsyncOperationsClient.java`, `TransferClient.java`,
  `SessionsClient.java`
- Auth implementation inspected: `ShareFileClientBuilder.java`, `TokenManager.java`,
  `HmacValidator.java`, `Credentials.java`

## Methodology

This audit compares the endpoints documented in `docs/API_REFERENCE.md` with the public SDK
connectors currently exposed by the Java SDK.

For this document, an endpoint is **Implemented** only when a public SDK method exposes the same
HTTP method and documented path. A helper that covers a similar workflow through a different path
or query shape is marked as **Partial / non-exact endpoint** and the documented endpoint remains
missing.

## Summary by Entity

| Entity               | Current SDK coverage |
| -------------------- | -------------------- |
| Authentication       | Partial              |
| Items                | Partial              |
| Users                | Partial              |
| Shares               | Partial              |
| Accounts             | Minimal              |
| Groups               | Missing              |
| AccessControls       | Mostly implemented   |
| Zones                | Missing              |
| Devices              | Missing              |
| WebhookSubscriptions | Missing              |
| AsyncOperations      | Partial              |

## Authentication

### Implemented

- `POST https://{subdomain}.{apicp}/oauth/token` for authorization-code grant.
- `POST https://{subdomain}.{apicp}/oauth/token` for password grant.
- `POST https://{subdomain}.{apicp}/oauth/token` for refresh-token grant.
- Bearer token usage in authenticated SDK requests.
- HMAC validation support in the auth layer.

### Missing

- `GET https://secure.sharefile.com/oauth/authorize` - no public connector builds the authorize
  redirect URL.

## Items

### Implemented

- `GET /sf/v3/Items({id})`
- `GET /sf/v3/Items({id})/TreeView`
- `GET /sf/v3/Items({id})/Stream`
- `GET /sf/v3/Items/ByPath`
- `GET /sf/v3/Items({id})/ByPath`
- `GET /sf/v3/Items({id})/Parent`
- `GET /sf/v3/Items({id})/Children`
- `GET /sf/v3/Items({id})/Info`
- `GET /sf/v3/Items({id})/Download`
- `GET /sf/v3/Items({id})/Thumbnail`
- `GET /sf/v3/Items({id})/Breadcrumbs`
- `GET /sf/v3/Items/Search`
- `GET /sf/v3/Items({id})/Search`
- `GET /sf/v3/Items({id})/DeletedChildren`
- `GET /sf/v3/Items/UserDeletedItems`
- `GET /sf/v3/Items/ByDlpStatus`
- `POST /sf/v3/Items({parentId})/Folder`
- `POST /sf/v3/Items({parentId})/Note`
- `POST /sf/v3/Items({parentId})/Link`
- `POST /sf/v3/Items({parentId})/BulkDownload`
- `POST /sf/v3/Items({id})/Copy`
- `POST /sf/v3/Items({id})/CheckOut`
- `POST /sf/v3/Items({id})/CheckIn`
- `POST /sf/v3/Items({id})/DiscardCheckOut`
- `POST /sf/v3/Items/AdvancedSearch`
- `POST /sf/v3/Items({parentId})/BulkDelete`
- `POST /sf/v3/Items/BulkRestore`
- `POST /sf/v3/Items({folderId})/Upload2`
- `PATCH /sf/v3/Items({id})`
- `DELETE /sf/v3/Items({id})`

### Missing

- `GET /sf/v3/Items`
- `GET /sf/v3/ConnectorGroups({id})/Children`
- `GET /sf/v3/Items({id})/WebView`
- `GET /sf/v3/Items({id})/ProtocolLinks({platform})`
- `GET /sf/v3/Items({id})/Redirection`
- `POST /sf/v3/Items({accountId})/SymbolicLink`
- `POST /sf/v3/ConnectorGroups({id})/Children`
- `POST /sf/v3/Items/AdvancedSimpleSearch`
- `POST /sf/v3/Items/BulkDeletePermanently`
- `POST /sf/v3/Items({id})/WebAppLink`
- `POST /sf/v3/Items({id})/RemoveTemplateAssociation`
- `POST /sf/v3/Items({id})/CheckVersioningViolation`
- `POST /sf/v3/Items({parentId})/CheckIfPreviewable`
- `POST /sf/v3/Items({folderId})/Upload`
- `PATCH /sf/v3/Items/Link({id})`
- `PATCH /sf/v3/Items/Note({id})`
- `PATCH /sf/v3/Items/SymbolicLink({id})`

### Partial / Non-Exact Endpoint

- Permanent bulk delete can be requested through `POST /Items({parentId})/BulkDelete` with a
  `deletePermanently` query parameter, but the exact documented
  `POST /sf/v3/Items/BulkDeletePermanently` endpoint is not exposed.

## Users

### Implemented

- `GET /sf/v3/Users`
- `GET /sf/v3/Users({id})`
- `POST /sf/v3/Users`
- `PATCH /sf/v3/Users({id})`
- `GET /sf/v3/Users({id})/Preferences`
- `PATCH /sf/v3/Users({id})/Preferences`
- `GET /sf/v3/Users({id})/Security`
- `DELETE /sf/v3/Users({id})`
- `GET /sf/v3/Users/AllSharedFolders`
- `GET /sf/v3/Users({id})/Groups`

### Missing

- `GET /sf/v3/Users?emailaddress={email}`
- `POST /sf/v3/UsersForFolder`
- `POST /sf/v3/Users/AccountUser`
- `PATCH /sf/v3/Users/AccountUser({id})`
- `PATCH /sf/v3/Users({id})/Roles`
- `PUT /sf/v3/Users({id})/Roles`
- `POST /sf/v3/Users({id})/RemoveRoles`
- `GET /sf/v3/Users({id})/HomeFolder`
- `GET /sf/v3/Users/HomeFolder`
- `GET /sf/v3/Users({id})/TopFolders`
- `GET /sf/v3/Users/TopFolders`
- `GET /sf/v3/Users({id})/Box`
- `GET /sf/v3/Users({id})/FileBox`
- `GET /sf/v3/Users/Preferences`
- `GET /sf/v3/Users/Security`
- `POST /sf/v3/Users/ResetPassword`
- `POST /sf/v3/Users/ForgotPassword`
- `POST /sf/v3/Users({id})/WelcomeNotification`
- `DELETE /sf/v3/Users/Clients`
- `POST /sf/v3/Users/Clients/BulkDelete`
- `POST /sf/v3/Users/Employees/Downgrade`
- `GET /sf/v3/Users({id})/AllSharedFolders`
- `GET /sf/v3/Users/NetworkShareConnectors`
- `GET /sf/v3/Users/SharepointConnectors`
- `POST /sf/v3/Users/Confirm`
- `GET /sf/v3/Users/Info`
- `POST /sf/v3/Users/DeleteEmailAddress?email={email}`
- `POST /sf/v3/Users/MakePrimary?email={email}`
- `POST /sf/v3/Users/SendConfirmationEmail?email={email}`
- `POST /sf/v3/Users/SendEmailVerificationCode?email={email}`
- `POST /sf/v3/Users/VerifyEmailAddress?email={email}&verificationCode={code}`
- `POST /sf/v3/Users/WebAppLink`
- `GET /sf/v3/Users/InboxMetadata`
- `GET /sf/v3/Users({id})/InboxMetadata`
- `GET /sf/v3/Users/Inbox`
- `GET /sf/v3/Users({id})/Inbox`
- `GET /sf/v3/Users/SentMessages`
- `GET /sf/v3/Users({id})/SentMessages`
- `POST /sf/v3/Users({id})/WebAppManageUser`
- `POST /sf/v3/Users/WebAppManageUsers`
- `POST /sf/v3/Users/WebAppAddEmployee`

### Partial / Non-Exact Endpoint

- `UsersClient#getByEmail` uses an OData filter against `GET /Users`, not the documented
  `GET /sf/v3/Users?emailaddress={email}` endpoint.
- `UsersClient#resetPassword` calls `POST /Users({id})/ResetPassword`, not the documented
  `POST /sf/v3/Users/ResetPassword` endpoint.
- `UsersClient#sendWelcomeEmail` calls `POST /Users({id})/ResendWelcome`, not the documented
  `POST /sf/v3/Users({id})/WelcomeNotification` endpoint.

## Shares

### Implemented

- `GET /sf/v3/Shares`
- `GET /sf/v3/Shares({id})`
- `GET /sf/v3/Shares({id})/Recipients`
- `GET /sf/v3/Shares({id})/Items`
- `GET /sf/v3/Shares({shareId})/Download`
- `POST /sf/v3/Shares`
- `PATCH /sf/v3/Shares({id})`
- `DELETE /sf/v3/Shares({id})`
- `POST /sf/v3/Shares({id})/Upload2`

### Missing

- `GET /sf/v3/Shares({shareId})/Recipients({recipientId})`
- `POST /sf/v3/Shares({shareId})/Recipients`
- `GET /sf/v3/Shares({shareId})/Items({itemId})`
- `GET /sf/v3/Shares({shareId})/Items({itemId})/Thumbnail`
- `GET /sf/v3/Shares({shareId})/Items({itemId})/ProtocolLinks({platform})`
- `GET /sf/v3/Shares({shareId})/Recipients({aliasId})/DownloadWithAlias`
- `POST /sf/v3/Shares({shareId})/Recipients({aliasId})/BulkDownload`
- `POST /sf/v3/Shares({id})/Alias`
- `DELETE /sf/v3/Shares({id})/Alias?email={email}`
- `POST /sf/v3/Shares/Send`
- `POST /sf/v3/Shares/SendCallback`
- `POST /sf/v3/Shares/Request`
- `POST /sf/v3/Shares/Resend`
- `GET /sf/v3/Shares({id})/Redirection`
- `GET /sf/v3/Shares/Inbox`
- `GET /sf/v3/Shares/Inbox({id})`
- `GET /sf/v3/Shares({shareId})/Recipients({aliasId})/Message`

### Partial / Non-Exact Endpoint

- `SharesClient#createSendShare` and `SharesClient#createRequestShare` both use
  `POST /sf/v3/Shares`; they do not expose the exact documented `POST /sf/v3/Shares/Send` or
  `POST /sf/v3/Shares/Request` endpoints.
- `SharesClient#sendNotification` calls `POST /Shares({id})/Notify`; that endpoint is not present
  in `docs/API_REFERENCE.md`.

## Accounts

### Implemented

- `GET /sf/v3/Accounts`

### Missing

- `GET /sf/v3/Accounts({id})`
- `GET /sf/v3/Accounts/Branding`
- `PATCH /sf/v3/Accounts/Branding`
- `GET /sf/v3/Accounts/Employees`
- `GET /sf/v3/Accounts/Clients`
- `GET /sf/v3/Accounts/AddressBook`
- `GET /sf/v3/Accounts/MobileSecuritySettings`
- `GET /sf/v3/Accounts/ProductDefaults`
- `GET /sf/v3/Accounts/Preferences`
- `PATCH /sf/v3/Accounts/Preferences`
- `GET /sf/v3/Accounts/SSO`
- `PATCH /sf/v3/Accounts/SSO`
- `POST /sf/v3/Accounts/SendToEmail?email={email}`
- `GET /sf/v3/Accounts/LoginAccessControlDomains`
- `GET /sf/v3/Accounts/FolderAccessControlDomains`
- `POST /sf/v3/Accounts/LoginAccessControlDomains`
- `POST /sf/v3/Accounts/FolderAccessControlDomains`
- `PATCH /sf/v3/Accounts/LoginAccessControlDomains`
- `PATCH /sf/v3/Accounts/FolderAccessControlDomains`
- `DELETE /sf/v3/Accounts/LoginAccessControlDomains`
- `DELETE /sf/v3/Accounts/FolderAccessControlDomains`
- `GET /sf/v3/Accounts/RequireWebPop`
- `GET /sf/v3/Accounts/RequireSubdomain`
- `POST /sf/v3/Accounts/FindSubdomain`
- `GET /sf/v3/Accounts/OutlookInformation`
- `GET /sf/v3/Accounts/SSOInfo?subdomain={subdomain}`
- `GET /sf/v3/Accounts/Tenants`
- `GET /sf/v3/Accounts/Tenants({id})`
- `GET /sf/v3/Accounts/Tenants/ZoneUsage`
- `GET /sf/v3/Accounts/Tenants({id})/Zones`
- `POST /sf/v3/Accounts/WebAppAdmin`

## Groups

`GroupsClient` is currently a placeholder, so all Groups endpoints in `docs/API_REFERENCE.md` are
missing.

### Missing

- `GET /sf/v3/Groups`
- `GET /sf/v3/Groups({id})`
- `POST /sf/v3/Groups`
- `PATCH /sf/v3/Groups({id})`
- `DELETE /sf/v3/Groups({id})`
- `GET /sf/v3/Groups({id})/Contacts`
- `POST /sf/v3/Groups({id})/Contacts`
- `DELETE /sf/v3/Groups({id})/Contacts`
- `GET /sf/v3/Groups({id})/ExportDocument`
- `GET /sf/v3/Users({id})/Groups`

## AccessControls

### Implemented

- `GET /sf/v3/AccessControls(principalid={principalId},itemid={itemId})`
- `GET /sf/v3/Items({id})/AccessControls`
- `POST /sf/v3/Items({id})/AccessControls`
- `PATCH /sf/v3/Items({id})/AccessControls`
- `DELETE /sf/v3/AccessControls(principalid={principalId},itemid={itemId})`
- `POST /sf/v3/Items({id})/AccessControls/BulkSet`
- `POST /sf/v3/AccessControls/BulkSetForPrincipal?principalId={principalId}`
- `POST /sf/v3/AccessControls/Clone`
- `POST /sf/v3/Items({id})/AccessControls/BulkDelete`
- `POST /sf/v3/Items({id})/AccessControls/NotifyUsers`

### Missing

- `POST /sf/v3/AccessControls/BulkDeleteForPrincipal?principalId={principalId}`
- `POST /sf/v3/Items({id})/AccessControls/NotifyUsersPreview`

## Zones

`ZonesClient` is currently a placeholder, so all Zones endpoints in `docs/API_REFERENCE.md` are
missing.

### Missing

- `GET /sf/v3/Zones`
- `GET /sf/v3/Zones({id})`
- `POST /sf/v3/Zones`
- `PATCH /sf/v3/Zones({id})`
- `DELETE /sf/v3/Zones({id})`
- `POST /sf/v3/Zones({id})/ResetSecret`
- `GET /sf/v3/Zones({id})/Tenants`
- `POST /sf/v3/Zones({id})/Tenants?accountId={accountId}`
- `DELETE /sf/v3/Zones({id})/Tenants({tenantId})`
- `GET /sf/v3/Zones({id})/Metadata`
- `POST /sf/v3/Zones({id})/Metadata`
- `DELETE /sf/v3/Zones({id})/Metadata?name={metadataName}`

## Devices

No `DevicesClient` is currently exposed, so all Devices endpoints in `docs/API_REFERENCE.md` are
missing.

### Missing

- `GET /sf/v3/Devices`
- `GET /sf/v3/Devices({id})`
- `GET /sf/v3/User({userId})/Devices`
- `DELETE /sf/v3/User({userId})/Devices({deviceId})`
- `POST /sf/v3/Devices({deviceId})/Wipe`
- `POST /sf/v3/Devices({deviceId})/Lock`
- `POST /sf/v3/Devices({deviceId})/Unlock`

## WebhookSubscriptions

`WebhookSubscriptionsClient` is currently a placeholder, so all WebhookSubscriptions endpoints in
`docs/API_REFERENCE.md` are missing.

### Missing

- `GET /sf/v3/WebhookSubscriptions`
- `GET /sf/v3/WebhookSubscriptions({id})`
- `POST /sf/v3/WebhookSubscriptions`
- `DELETE /sf/v3/WebhookSubscriptions({id})`

## AsyncOperations

### Implemented

- `GET /sf/v3/AsyncOperations({id})`

### Missing

- `GET /sf/v3/AsyncOperations/GetByBatch({batchId})`
- `GET /sf/v3/AsyncOperations/GetBatch({batchId})`
- `GET /sf/v3/AsyncOperations/GetByFolder({folderId})`
- `POST /sf/v3/AsyncOperations({id})/Cancel`
- `POST /sf/v3/AsyncOperations/CancelBatch({batchId})`
- `PATCH /sf/v3/AsyncOperations({id})`
- `DELETE /sf/v3/AsyncOperations({id})`

### Partial / Non-Exact Endpoint

- `AsyncOperationsClient#list` exposes `GET /sf/v3/AsyncOperations`, which is not documented in
  `docs/API_REFERENCE.md`.
