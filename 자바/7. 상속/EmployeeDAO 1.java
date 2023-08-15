package test.jdbc.oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Scanner;

public class EmployeeDAO {

	private Statement stmt;
	private Connection conn;
	private ConnectionPool pool;

	/**
	 * 과제 1 : update 복습 & delete 적용, 고민(insert(), select...()도 적용 여부)
	 * 
	 * @param stmt
	 * @param id
	 * @param in
	 * @return
	 * @throws SQLException
	 */
	public int update(EmployeeDTO emp) throws SQLException {
		// TODO Auto-generated method stub
		conn = getConnection();
		stmt = conn.createStatement();
		conn.setAutoCommit(false);
		String sql = "UPDATE EmpTBL SET pwd = '"+emp.getPwd()+"', "
					    + "name = '"+emp.getName()+"', "
					    + "phone = '"+emp.getPhone()+"', "
					    + "email = '" +emp.getEmail()+"'"
					    +" WHERE  id = '"+emp.getId()+"'";
		int result = stmt.executeUpdate(sql);
		if(result == 1) {
			conn.commit();
		}else {//같은 id가 여러명 또는 존재하지 않음
			conn.rollback(); // DB서버에서 수행되는 일
		}
		conn.setAutoCommit(true);
		close();
		return result;
	}

	public int insert(EmployeeDTO emp) throws SQLException {
		// TODO Auto-generated method stub
		conn = getConnection();
		stmt = conn.createStatement();
		String sql = "INSERT INTO  EmpTBL (seq, id, pwd, name, phone, email, hireDT )\n" + 
				" VALUES  (seq_EmpTBL.NEXTVAL, '"+emp.getId()+"', '"+emp.getPwd()+"', '"+emp.getName()+"', '"+emp.getPhone()+"', '"+emp.getEmail()+"', SYSDATE )";
		int result = stmt.executeUpdate(sql);
		close();
		return result;
	}
	/**
	 * 직원 정보를 삭제한 뒤 정상적으로 삭제되었는지를 확인한다.
	 * 1번 과제에서 수정할 함수...
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public  int delete(String id) throws SQLException {
		// TODO Auto-generated method stub
		conn = getConnection();
		stmt = conn.createStatement();
		EmployeeDTO emp;
		String returnResult;
		String sql = "DELETE FROM  EmpTBL WHERE id = '"+id+"'";
		int result = stmt.executeUpdate(sql);
		//		5. 결과테이블에서 데이터 추출
		
//		emp = selectById(id);
		if(result == 1) {
				conn.commit();
		}else {
			conn.rollback();
		}
		close();
		return result;
	}
	public  EmployeeDTO selectById(String id) throws SQLException {
		// TODO Auto-generated method stub
		conn = getConnection();
		stmt = conn.createStatement();
		String sql = "select * from EmpTBL" + " where id = '"+id+"'";
		ResultSet result = stmt.executeQuery(sql);
		EmployeeDTO emp;
	//	5. 결과테이블에서 데이터 추출
		if(result.next()) {
			emp = new EmployeeDTO(result.getString("id"), result.getString("pwd"), result.getString("name"), 
					result.getString("phone"), result.getString("email"), result.getString("hireDT"));
		}else
			emp = null;
		result.close();
		close();
		return emp;
	}
	
	public  EmployeeDTO selectByName(String name) throws SQLException {
		// TODO Auto-generated method stub
		conn = getConnection();
		stmt = conn.createStatement();
		String sql = "select * from EmpTBL" + " where name = '"+name+"'";
		System.out.println("selectByName()::\n" + sql);
		ResultSet result = stmt.executeQuery(sql);
		EmployeeDTO emp;
	//	5. 결과테이블에서 데이터 추출
		if(result.next()) {
			String id = result.getString("id");
			String pwd = result.getString("pwd");
			name = result.getString("name");
			String phone = result.getString("phone");
			String email = result.getString("email");
			String hireDT = result.getString("hireDT"); // Date
			//레코드 한개를 하나의 객체로 만들자.
			emp = new EmployeeDTO(id, pwd, name, phone, email, hireDT);
	//		emps[i++] = emp; //++i : 먼저 증가, i++ : i를 가지고 명령실행 후 i가 증가
		}else
			emp = null;
		result.close();
		close();
		return emp;
	}
	
	public  ArrayList<EmployeeDTO> selectAll() throws SQLException {
		// TODO Auto-generated method stub
		conn = getConnection();
		stmt = conn.createStatement();
		String sql = "select * from EmpTBL";
		ResultSet result = stmt.executeQuery(sql);
		
	ArrayList<EmployeeDTO> listEmp = new ArrayList<EmployeeDTO>();
		//	5. 결과테이블에서 데이터 추출
		while(result.next()) {
			String id = result.getString("id");
			String pwd = result.getString("pwd");
			String name = result.getString("name");
			String phone = result.getString("phone");
			String email = result.getString("email");
			String hireDT = result.getString("hireDT"); // Date
			//레코드 한개를 하나의 객체로 만들자.
			EmployeeDTO emp = new EmployeeDTO(id, pwd, name, phone, email, hireDT);
			listEmp.add(emp);
	//		emps[i++] = emp; //++i : 먼저 증가, i++ : i를 가지고 명령실행 후 i가 증가
		}
		result.close();
		close();
		return listEmp;
	}
	public Connection getConnection() throws SQLException{
		return pool.getConnection();
	}
	public void close() throws SQLException {
		//6. 연결 해제 : close()
		stmt.close();
		pool.releaseConnection(conn);
	}
	
	public EmployeeDAO() {}
	
	public ConnectionPool getConnectionPool() {
		//2. 연결얻어오기
		String url = "jdbc:oracle:thin:@localhost:1521:xe"; // www.u-anyware.com
		String user = "hr";
		String password = "hr";
		try {
			pool = new ConnectionPool(url, user, password, 3, 5, true, 10);
//			conn = pool.getConnection(true, 5);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("DBMS 정보 확인 필요!!!\n"
					+ url + "\n" + user + "\n" + password);
		}
//		//3. 명령문 생성하기
//		try {	stmt = conn.createStatement();
//		} catch (SQLException e) {
//			e.printStackTrace();
//			while (true) {
//				try {	Thread.sleep(300);	} catch (InterruptedException e1) {
//					e1.printStackTrace();		}
//				try {	stmt = conn.createStatement();
//				} catch (SQLException e1) {
//					System.out.println(e1.getMessage());
//				}
//			}
//		}
		return pool;
	}
}
