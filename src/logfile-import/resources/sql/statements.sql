-- name: create-log-entry!
-- insert apache log file entry
INSERT INTO statistics (type_, branch_code, date) VALUES (:type_, :branch_code, :date)

-- name: get-branch-codes
-- gets all branch-codes
SELECT code from branch