package models.lottery.statistics

object StatisticsSQL {

	private[statistics] val insertSql = "INSERT INTO statistics (type_, branch_code, date) VALUES ({type}, {branch_code}, {date})"

	private[statistics] val sqlByLottery = """SELECT s.id, type_, date
											          FROM statistics s, branch b
		                            WHERE b.code= s.branch_code
															  AND lottery_id = {lid}
		                            ORDER BY s.id desc"""

	private[statistics] val selectDrawing = """ SELECT YEAR(date) year , MONTH(date) month, count(*) count
		                                FROM statistics s, branch b
		                                WHERE b.code = s.branch_code"""

	private[statistics] val tailDrawings = """AND type_ = {type}
			                            GROUP BY YEAR (date), MONTH(date)
	                                ORDER BY date desc"""

	private[statistics] val hits = selectDrawing + " AND lottery_id = {lid} " + tailDrawings

	private[statistics] val hitsPerBranchDrawing = selectDrawing + " AND branch_code = {bcode} " + tailDrawings

	private[statistics] val selectUsers = """SELECT YEAR(created) year , MONTH(created) month, count(*) count
		                             FROM client c, branch b
		                             WHERE b.code = c.branch_code"""

	private[statistics] val lot = " AND lottery_id = {lid}"

	private[statistics] val lotUnregisteredUsers = " WHERE lottery_id = {lid}"

	private[statistics] val tailUsers = " GROUP BY YEAR( created ), MONTH( created ) ORDER BY created desc"

	private[statistics] val tailUnregisteredUsers = " GROUP BY YEAR( date_deleted ), MONTH( date_deleted ) ORDER BY date_deleted desc"

	private[statistics] val hitsPerBranchUsers = " AND branch_code = {bcode} "

	private[statistics] val whereUnregisteredUsers = " WHERE branch_code in (SELECT code from branch where lottery_id = {lid})"

	private[statistics] val selectUnregisteredUsers = "SELECT YEAR(date_deleted) year , MONTH(date_deleted) month, count(*) count FROM client_unregister_history"

	private[statistics] val unregisteredPerBranchUsers = " WHERE branch_code = {bcode} "

}
