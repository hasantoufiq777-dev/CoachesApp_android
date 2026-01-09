# Transfer System Documentation

## Overview
The transfer system allows players to request transfers either to a specific club or to the general market. Managers can approve requests and set release fees for market transfers, while other managers can purchase players from the market.

## Features Implemented

### 1. Transfer Request Submission (Player Role)
**Location**: `TransferRequestActivity.java` - `showNewRequestDialog()`

When a player clicks the "New Request" button, they can choose between two transfer types:

#### Option 1: Transfer to Specific Club
- Player selects a destination club from available clubs
- Creates a `DIRECT_CLUB` transfer request
- Status: `PENDING_APPROVAL`
- Requires manager approval from source club
- Upon approval, transfer completes immediately and player moves to destination club

#### Option 2: Transfer to General Market
- No destination club is selected (destinationClubId = null)
- Creates a `GENERAL_MARKET` transfer request
- Status: `PENDING_APPROVAL`
- Requires manager approval with release fee setting
- Upon approval, status changes to `IN_MARKET` and becomes available for purchase

### 2. Manager Approval Process
**Location**: `TransferRequestActivity.java` - `approveTransferRequest()`

Manager approval workflow differs based on transfer type:

#### Direct Club Transfer Approval
- Manager confirms the transfer
- Status changes to `COMPLETED`
- Player's clubId is updated to destination club
- Transfer completes immediately

#### General Market Transfer Approval
- Manager must set a release fee
- Shows dialog to input release fee (must be > 0)
- Status changes to `IN_MARKET`
- Player remains with current club until purchased
- Release fee is saved with the transfer request

### 3. Transfer Market
**Location**: `TransferMarketActivity.java`

#### Features:
- Shows all transfers with `IN_MARKET` status
- Displays player details: name, position, age, jersey number
- Shows current club and release fee
- Position filter to search for specific player positions
- Only managers can purchase players (admin and players can only view)

#### Purchase Process:
- Manager clicks "Purchase" button on a player
- Confirms purchase with release fee amount
- Validation:
  - Must be a manager with a club assigned
  - Cannot purchase from own club
- Upon confirmation:
  - Transfer status changes to `COMPLETED`
  - Destination club set to purchasing manager's club
  - Transfer fee set to release fee amount
  - Player's clubId updated to new club

### 4. Admin Restrictions
**Location**: `TransferRequestActivity.java` - Adapter `onBindViewHolder()`

#### Admin Role Limitations:
- Can VIEW all transfers in the system
- CANNOT approve or reject any transfers
- Action buttons (Approve/Reject) are hidden for admin users
- Only `CLUB_MANAGER` role can see and use action buttons
- Admin serves as observer/auditor of transfer activities

## Data Model Updates

### TransferRequest.java
New fields added:
- `transferType` (enum: DIRECT_CLUB, GENERAL_MARKET)
- `releaseFee` (Double)

Constructor automatically sets transferType based on destinationClubId:
- If destinationClubId is null → `GENERAL_MARKET`
- If destinationClubId is set → `DIRECT_CLUB`

### TransferStatus Enum
- `PENDING_APPROVAL`: Initial status, awaiting manager approval
- `IN_MARKET`: Market transfer approved, available for purchase
- `COMPLETED`: Transfer finalized, player moved to new club
- `CANCELLED`: Transfer request cancelled

## User Workflows

### Player Workflow
1. Navigate to Transfer Requests screen
2. Click "New Request" button
3. Select transfer type (Specific Club or General Market)
4. If specific club: Select destination club from list
5. Submit request
6. Wait for manager approval
7. If approved for market: Player appears in Transfer Market
8. If direct transfer approved: Player moves to destination club immediately

### Manager Workflow (Source Club)
1. View pending transfer requests for their club
2. Review player transfer request
3. For direct club transfers:
   - Approve: Player moves immediately
   - Reject: Transfer cancelled
4. For general market transfers:
   - Approve: Enter release fee → Player enters market
   - Reject: Transfer cancelled

### Manager Workflow (Purchasing from Market)
1. Navigate to Transfer Market
2. Browse available players
3. Use position filter to find specific positions
4. View player details and release fee
5. Click "Purchase" on desired player
6. Confirm purchase
7. Player joins manager's club immediately

### Admin Workflow
1. View all transfers across all clubs
2. Monitor transfer activity
3. No ability to approve, reject, or purchase
4. Serves as audit/oversight role

## Technical Implementation Details

### Key Methods

#### TransferRequestActivity
- `showNewRequestDialog()`: Displays transfer type selection dialog
- `showClubSelectionDialog()`: Lists available clubs for direct transfer
- `showGeneralMarketRequestDialog()`: Confirms general market request
- `submitDirectClubTransfer()`: Creates direct club transfer request
- `submitGeneralMarketTransfer()`: Creates market transfer request
- `approveTransferRequest()`: Handles approval based on transfer type
- `showReleaseFeeDialog()`: Prompts manager for release fee
- `approveMarketTransfer()`: Approves market transfer with fee

#### TransferMarketActivity
- `loadMarketPlayers()`: Loads all IN_MARKET transfers
- `filterMarketPlayers()`: Filters by position
- `showPurchaseDialog()`: Confirms purchase with fee display
- `completePurchase()`: Processes purchase and updates player club

### Validation Rules
1. Player must have a valid profile to submit transfer
2. Player cannot transfer to their current club
3. Manager can only approve transfers from their club
4. Manager cannot purchase from their own club
5. Release fee must be greater than 0
6. Admin cannot interact with transfers (view only)

## Database Operations

### On Transfer Request Submit
- Create new TransferRequest document
- Set status to PENDING_APPROVAL
- Set transferType based on destination club
- Store player and club names for display

### On Manager Approval (Direct)
- Update status to COMPLETED
- Update player's clubId to destination club
- Set completion date

### On Manager Approval (Market)
- Update status to IN_MARKET
- Store release fee
- Set approval date
- Player remains with current club

### On Market Purchase
- Update status to COMPLETED
- Set destination club to purchasing manager's club
- Set transfer fee to release fee
- Update player's clubId to purchasing club
- Set completion date

## UI Components

### Transfer Request Item (item_transfer_request.xml)
- Player name
- Source club
- Destination club (or "Transfer Market")
- Transfer/Release fee
- Status badge (color-coded)
- Request date
- Action buttons (Approve/Reject) - conditional visibility

### Market Player Item (item_market_player.xml)
- Player name, position, age, jersey
- Current club
- Release fee
- Purchase button (managers only)

## Future Enhancements (Not Implemented)
- Negotiation system for transfer fees
- Player contract expiration
- Transfer deadline windows
- Multi-step approval (destination club manager approval)
- Transfer history tracking
- Financial transactions and club budgets
- Player statistics affecting release fees
