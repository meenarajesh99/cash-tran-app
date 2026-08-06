-- Move transfers using duplicate Pending to the original Pending
UPDATE transfer
SET transfer_status_id = 1
WHERE transfer_status_id = 4;


-- Move transfers using Approved to Completed
UPDATE transfer
SET transfer_status_id = 5
WHERE transfer_status_id = 2;


-- Move transfers using duplicate Rejected to original Rejected
UPDATE transfer
SET transfer_status_id = 3
WHERE transfer_status_id = 6;


-- Remove duplicate statuses
DELETE FROM transfer_status
WHERE transfer_status_id IN (2,4,6);