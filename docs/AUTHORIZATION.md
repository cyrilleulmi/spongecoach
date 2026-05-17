# Authorization Model

This document is the authoritative definition of what users can do in SpongeCoach. Read it before implementing any permission check, UI action, or API endpoint.

## Roles

Roles are **club-scoped**. There is no global/app-level role.

| Role | Meaning |
|---|---|
| `ADMIN` | Full write access within the club |
| `MEMBER` | Read-only access within the club |

All admins are equal — there is no "founding admin" or "owner" distinction. Any admin can act on any other member, including other admins.

## Screens & Navigation

| Route | Description |
|---|---|
| `/clubs` | List of clubs the authenticated user belongs to |
| `/clubs/:id` | Club detail — two tabs: **Teams** (default) and **Members** |
| `/teams/:id` | Team detail — member list |
| `/profile` | Authenticated user's own profile, always accessible from top nav |

There is **no global persons list**. Members are always viewed in context of a club or team.

## Visibility Rules

- A user can only see clubs they belong to — enforced at the API level, not just the UI.
- Within a club, all members (ADMIN and MEMBER alike) can see:
  - The full club member list
  - All teams in the club
  - The member list of every team in the club
- No user can see persons outside their club(s).

## Action Matrix

### Club-level actions

| Action | ADMIN | MEMBER |
|---|---|---|
| Create a club (any authenticated user) | ✓ | ✓ |
| View club details & member list | ✓ | ✓ |
| Edit club details (name, description, …) | ✓ | ✗ |
| Change a member's role (MEMBER ↔ ADMIN) | ✓ | ✗ |
| Remove a member from the club | ✓ | ✗ |
| Delete the club | ✗ (not implemented) | ✗ |

### Team-level actions

| Action | ADMIN | MEMBER |
|---|---|---|
| Create a team (within own club) | ✓ | ✗ |
| View team details & member list | ✓ | ✓ |
| Edit team details | ✓ | ✗ |
| Add a club member to a team | ✓ | ✗ |
| Remove a member from a team | ✓ | ✗ |
| Delete a team | ✓ | ✗ |

### Account-level actions

| Action | Any authenticated user |
|---|---|
| View own profile | ✓ |
| Edit own profile data | ✓ |
| Delete own account | ✓ (see guard below) |
| Delete another user's account | ✗ (never) |

## Cascade & Guard Rules

### Removing a member from a club
- The member is automatically removed from **all teams** within that club.
- Before confirming, the UI must show a dialog listing the affected teams.

### Deleting a team
- All team memberships for that team are removed.
- Club memberships are **not** affected.

### Deleting own account
- All club and team memberships are removed.
- **Blocked** if the user is the last admin of any club. The error must name the club(s) and instruct the user to assign another admin first.

### Adding someone to a team
- The person must already be a club member. A non-member cannot be added directly to a team.

## Confirmation Dialogs

Every destructive action requires a confirmation dialog before executing:
- Remove member from team
- Remove member from club (dialog lists affected teams)
- Delete team
- Delete account

## Data ownership

- Only the user themselves can edit their own profile data.
- Future: club- or team-specific data per member may be editable by club admins — but this is **not in scope yet**. Do not implement it.

## What is out of scope (defer these)

- Invite links or self-join flows — member addition is admin-only for now.
- Club deletion — endpoint returns 405 Method Not Allowed; keep it that way.
- Global admin / superuser role — will never exist by design.
