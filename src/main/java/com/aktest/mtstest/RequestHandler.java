package com.aktest.mtstest;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class RequestHandler implements Runnable {
	private static final int HTTP_TASK_POSTED = 202;
	private static final int HTTP_NO_SUCH_TASK = 404;
	private static final int HTTP_SERV_ERROR = 500;
	private static final int HTTP_INVAL_GUID = 400;
	private static final int HTT_TASK_STAT = 200;
	protected static final int HTTP_PORT=40400;
	protected static final int TWO_MINUTES=2*60*1000;
	protected static Connection connect;
	protected static Gson gsonExt;
	protected static ArrayBlockingQueue<Task> tasksToStop = new ArrayBlockingQueue<>(20000);
	protected static ArrayBlockingQueue<Task> tasksToStart = new ArrayBlockingQueue<>(20000);
	
	static {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(Timestamp.class, new DateAdaptor());
		gsonExt = gson.create();
	}

	// Task finisher thread
	public void run() {
		Thread.currentThread().setName("TaskStartThread");
		while(true) {
			try {
				Task t = tasksToStart.take();
				System.out.println("New task:" + t);
				java.util.Date now= new java.util.Date();
				if ("created".equals(t.getStatus())) {
					t.setStatus("running");
					updateTask(t);
					tasksToStop.put(t);
				}
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}		
	}
	
	public void runTaskStarterThread() {
		Thread.currentThread().setName("TaskStopThread");
		while(true) {
			try {
				Task t = tasksToStop.take();
				if ("running".equals(t.getStatus())) {
					System.out.println("Running task:" + t);
					java.util.Date now= new java.util.Date();
					long timeToSleep =  (t.getTimestamp().getTime() + TWO_MINUTES) - now.getTime();
					System.out.println("Waiting for task stop:" + timeToSleep);
					Thread.sleep(timeToSleep);
					t.setStatus("finished");
					System.out.println("Task stopped:" + t);
					updateTask(t);
				}
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}
	}	
	
	private void updateTask(Task t) throws SQLException {
		synchronized (connect) {
			try(PreparedStatement st = connect.prepareStatement("UPDATE task SET status=?, tm=? WHERE uid=?")) {
				st.setString(1, t.getStatus());
				st.setTimestamp(2, t.getTimestamp());
				st.setString(3, t.getUid());
				st.execute();
			}
		}
	}

	public static void main(String[] args) {
		try {
			initDatabase();
			loadOldTasks();
			RequestHandler rh = new RequestHandler();

			spark.Spark.port(HTTP_PORT);
			Spark.post("/task", postTask);
			Spark.get("/task/:uid", getTask);
			
			rh.runTaskStopperThread();
			rh.runTaskStarterThread();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void runTaskStopperThread() {
		new Thread(this).start();		
	}

	private static void loadOldTasks() throws SQLException, InterruptedException {
		synchronized (connect) {
			try(PreparedStatement st = connect.prepareStatement("SELECT * FROM task WHERE status<>'finished'")) {
				try(ResultSet rs = st.executeQuery()) {
					while(rs.next()) {
						Task t = new Task(rs);
						if ("created".equals(t.getStatus()))
							tasksToStart.add(t);
						else if ("running".equals(t.getStatus()))
							tasksToStop.add(t);
					}
				}
			}
		}
	}

	private static void initDatabase() throws SQLException {
		try {
			 if (org.hsqldb.jdbc.JDBCDriver.driverInstance == null) 
				 Class.forName("org.hsqldb.jdbc.JDBCDriver" );
		 } catch (Exception e) {
		     System.err.println("ERROR: failed to load HSQLDB JDBC driver.");
		     e.printStackTrace();
		     return;
		 }		
		connect = DriverManager.getConnection("jdbc:hsqldb:file:./testdb", "sa", "");
		try(PreparedStatement st = connect.prepareStatement("CREATE TABLE IF NOT EXISTS task(uid varchar(64) PRIMARY KEY, status varchar(32) default 'created', tm timestamp default CURRENT_TIMESTAMP);")) {
			st.execute();
		}
	}

	protected static Route postTask = (Request request, Response response) -> {
		try{
			Task t = new Task();
			synchronized (connect) {
				try(PreparedStatement st = connect.prepareStatement("INSERT INTO task(uid) VALUES(?)")) {
					st.setString(1, t.getUid());
					st.execute();
					response.status(HTTP_TASK_POSTED);
					tasksToStart.add(t);	
					return t.getUid();
				}
			}
		} catch(Throwable e) {
			response.status(HTTP_SERV_ERROR);
			return e.getMessage();
		}
	};	

	protected static Route getTask = (Request request, Response response) -> {
		String uidStr = request.params(":uid");
		System.out.println("Request task:" + uidStr);
		try {
			UUID.fromString(uidStr);
			synchronized (connect) {
				try(PreparedStatement st = connect.prepareStatement("SELECT * FROM task WHERE uid=?")) {
					st.setString(1, uidStr);
					try(ResultSet rs = st.executeQuery()) {
						if (rs.next()) {
							Task t = new Task(rs);
							System.out.println("Request task:" + uidStr + "-> " + t);
							response.status(HTT_TASK_STAT);
							return gsonExt.toJson(t);
						} else
							response.status(HTTP_NO_SUCH_TASK);
					}
				}
			}
		} catch(IllegalArgumentException x) {
			response.status(HTTP_INVAL_GUID);
		} catch(Throwable e) {
			response.status(HTTP_SERV_ERROR);
			return e.getMessage();
		}
		return "";
	};
}

