package com.bigsea.rest;

import java.io.*;

import java.nio.charset.Charset;
import java.util.Arrays;



import java.io.FileNotFoundException;


import java.util.Enumeration;
import java.util.Properties;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class Utilities {
	
    public ResultSet query(String dbName, Connection connect, String sqlStatement)
    {
    	ResultSet resultSet = null;
    	try
    	{
    		Statement statement = connect.createStatement();

    		resultSet = statement.executeQuery(sqlStatement);
    		
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	return resultSet; 
    }
    
    
    public void createConfigAppFile(Connection connection, String appId, String deadline) throws RuntimeException
    {
    	String sqlStatement, configApp = new String();
    	ResultSet resultSet = null;
    	
    	/* 
		 * 	Build the ConfigApp.txt
		 * select application_id, chi_0, chi_c, phi_mem,vir_mem, phi_mem,phi_core, vir_core 
		 * from APPLICATION_PROFILE_TABLE
		 * 
		 * 
		 */
		
		sqlStatement = "SELECT application_id, chi_0, chi_c, phi_mem,vir_mem, phi_core,vir_core FROM " + readWsConfig("AppsPropDB_dbName") +".APPLICATION_PROFILE_TABLE "+
				"WHERE application_id="+"'" + appId + "'";
		try
		{
			resultSet =  query(readWsConfig("AppsPropDB_dbName"), connection, sqlStatement);
			if (!resultSet.next())
			{
				throw new RuntimeException("Fatal error: SqlStatement " + sqlStatement+" returned 0 rows");
				
		
			}
			
			configApp = resultSet.getString("application_id").concat(" ");
			configApp = configApp.concat(String.valueOf(resultSet.getDouble("chi_0"))).concat(" ");
			configApp = configApp.concat(String.valueOf(resultSet.getDouble("chi_c"))).concat(" ");
			configApp = configApp.concat(String.valueOf(resultSet.getDouble("phi_mem"))).concat(" ");
			configApp = configApp.concat(String.valueOf(resultSet.getDouble("vir_mem"))).concat(" ");
			configApp = configApp.concat(String.valueOf(resultSet.getInt("phi_core"))).concat(" ");
			configApp = configApp.concat(String.valueOf(resultSet.getInt("vir_core"))).concat(" ");
			configApp = configApp.concat(deadline);
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}	
		
		BufferedWriter writer = null;
        try 
        {
            //create ConfigApp_1.txt file
            String filename = readWsConfig("RESOPT_HOME");
            File myFile = new File(filename.concat("/ConfigApp_1.txt"));

        
            writer = new BufferedWriter(new FileWriter(myFile));
            writer.write(configApp);
        } 
        
        catch (Exception e) 
        {
            e.printStackTrace();
            System.exit(-1);
        } 
        finally 
        {
            try 
            {

                writer.close();
            } catch (Exception e) 
            {
            }
        }


    }
    
    
    
    public void insert(String dbName, Connection connect, String sqlStatement) 
    {
    	try
    	{
    		Statement statement = connect.createStatement();

    		statement.executeUpdate(sqlStatement);
    		
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
      	}
 
    }
    
    /* Temporary solution: avg not interpolation */
    public double Interpolation(double x, double x1, double x2, int[] values)
    {
    	double res1;
    	double res2;
    	
    	res1 = (x - x1)/(x2 - x1);
    	res2 = values[1] - values[0];
    	
    	return values[0] + res1*res2;
    }
    
   
    public Connection readDataBase(String dbName, String Ip, String user, String password			
                    ) throws Exception 
    {
        Connection connect = null;
        try 
        {
           // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Setup the connection with the DB
            String conn_string = "jdbc:mysql://" + Ip +"/" + dbName + "?"
                    + "user=" + user + "&password="+ password;
            connect = DriverManager.getConnection(conn_string);             

        } 
        catch (Exception e) 
        {
            throw e;
        } 

        return connect;
    }
	
    public String writeResultSet(ResultSet resultSet) throws SQLException {
		String application_id = "";
		String dataset_size = "";
		String deadline = "";
		
        while (resultSet.next()) {

            application_id = resultSet.getString("application_id");
            dataset_size = resultSet.getString("dataset_size");
            deadline = resultSet.getString("deadline");

        }
        return application_id +" " + dataset_size + " "+ deadline;
    }
	
	
    public void close(Connection connect) {
        try {
            if (connect != null) {
                connect.close();
            }
        } catch (Exception e) {

        }
    }
	
    public String bestMatchProduct(File[] directories, String nNodesnCores, String dataset_size, String param, String app_id)
    {
        int min = Integer.MAX_VALUE;
        int diff = 0;
        int prod;
        String parameters = "";
        int newnNodes, newnCores;

        for (int i = 0; i < directories.length; i++)
        {	
            newnNodes = extract(directories[i].getName(), 1);
            newnCores = extract(directories[i].getName(), 2);
            prod =  newnNodes * newnCores;
            diff = Math.abs(Integer.valueOf(nNodesnCores) - prod);
            if (min > diff) 
            {
                parameters = String.valueOf(newnNodes) + " "+ String.valueOf(newnCores);
                min = diff;
            }
        }

       return parameters;
    }
	
    public String readWsConfig(String variable)
    {
        String filename = System.getenv("HOME") + "/" + "wsi_config.xml";
        
        try {
                File file = new File(filename);
                FileInputStream fileInput = new FileInputStream(file);
                Properties properties = new Properties();
                properties.loadFromXML(fileInput);
                fileInput.close();

                Enumeration enuKeys = properties.keys();
                while (enuKeys.hasMoreElements()) 
                {
                        String key = (String) enuKeys.nextElement();
                        String value = properties.getProperty(key);
                        //System.out.println(key + ": " + value);
                        if (key.equals(variable)) return(value);
                }
        } catch (FileNotFoundException e) {
                e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
        }
        
        return("Error: variable not found in wsi_cnfig.xml file");
    }
		

	
    public String bestMatch(File[] directories, String nNodes, String nCores, String dataset_size, String param, String app_id)
    {

        int min = Integer.MAX_VALUE;
        int diff = 0;
        int position = 1;
        String pattern;

        pattern = nNodes + "_" + nCores + "_" + dataset_size + "_" + param +
                    "/" + app_id;
        boolean  nodesFound = false, coresFound = false;
        int saveIndex[] = new int[2];
        String newNNodes = "", newNCores = "";

        String parameters = nNodes + " " + nCores + " ";



         // Find the best match. Consider first number of nNodes then nCores
        for (position = 0; position <2; position++)
        {	  
            min = Integer.MAX_VALUE;
            for (int i = 0; i < directories.length; i++)
            {	    

                diff =  Math.abs((extract(pattern, position+1) - extract(directories[i].getName(), position+1)));
                // diff == 0 means the two parameters match
                if (diff == 0) 
                {
                        if (position+1 == 1) nodesFound = true;
                        else if (position+1 == 2) coresFound = true;
                        else System.exit(-1);
                        break; // pass to the other parameters or quit
                }
                else
                        if (min > diff) 
                        {
                                saveIndex[position] = i;
                                min = diff;
                        } 
            }
        }

        // Prepare the new parameters
        if (!nodesFound) newNNodes = String.valueOf(extract(directories[saveIndex[0]].getName(), 1));
        if (!coresFound) newNCores = String.valueOf(extract(directories[saveIndex[1]].getName(), 2));

        if (coresFound && !nodesFound) // it means a match for nNodes has been found
                    parameters = Integer.valueOf(newNNodes) + " " + Integer.valueOf(nCores) + " ";
                else // it means a match for nCores has been found
                        if (!coresFound && nodesFound)  parameters = nNodes + " " + newNCores + " "; 
                                else // It means a match for nNodes has been found
                                        if (!coresFound && !nodesFound) parameters = newNNodes + " " + newNCores + " ";

         return parameters;
    }

	
	
    public String Start(String path, String luafilename)
    {
        String cmd, errorMsg = new String("ERROR: could not open lua file. Check .demo_properties file");
        cmd = "cd ".concat(path).concat(";").concat("./dagsim.sh ");

        cmd = cmd.concat(luafilename).concat(" 2>&1|sed -n 1,1p|awk '{print $3\" \" $4\" \" $5\" \" $6\" \" $7}'");
        //msg = _run("cd /Users/Enrico/Dropbox/Developement/C/DagSim/;./dagSim test1.lua 2>&1|sed -n 1,1p|awk '{print $3\" \" $4\" \" $5\" \" $6\" \" $7}'");

        errorMsg = _run(cmd);



        if (errorMsg.contains("ERROR")) return(cmd.concat(" ").concat(errorMsg));//return ("ERROR: could not open lua file. Check .demo_properties file");
        else return errorMsg;

        // return ("cmd = ".concat(cmd));
    }
        
    /**
     * Start dagsim with the -s option
     * @param path path to dagSim folder
     * @param luafilename path to the lua model description
     * @return The complete dagSim output
     */
    public String StartDagsimStages(String path, String luafilename) {
        String cmd = "cd " + path + ";./dagsim.sh " + luafilename + " -s 2>&1";

        String outputMsg = _run(cmd);

        if (outputMsg.contains("ERROR"))
            return "ERROR: could not open lua file. Check -demo_properties file";
        else
            return outputMsg;
    }

    /**
     * Filter the list of String containing tag
     * @param lines
     * @param tag
     * @return A new list of strings, all containing the tag.
     */
    public String[] filterLines(String[] lines, String tag) {
        int count = 0;
        for (String line : lines) {
            if (line.contains(tag))
                count++;
        }

        String[] filteredLines = new String[count];
        int index=0;
        for (String line : lines) {
            if (line.contains(tag))
            {
                filteredLines[index] = line;
                index++;
            }
        }

        return filteredLines;
    }

    /**
     * Reads the dagSim output with option -s and returns the estimated remaining time from currentStage
     * @param dagsimOutput
     * @param currentStage
     * @return estimated remaining time
     */
    public long getRemainingTime(String dagsimOutput, String currentStage) {            
        long totalTime = getTotalExecutionTime(dagsimOutput);
        long stageWaitTime = getStageWaitTime(dagsimOutput, currentStage);


        return totalTime - stageWaitTime;
    }

    /**
     * From the full dagsimOutput extracts the total time (3rd row, 3rd column)
     * @param dagsimOutput
     * @return 
     */
    public long getTotalExecutionTime(String dagsimOutput) {
        return (long)Math.floor(Double.valueOf(extract(dagsimOutput.split("\n")[2], 3, "\t")));
    }

    /**
     * From the full dagsimOutput extracts the stage wait time (2nd row, 4th column of the stage-relative rows)
     * @param dagsimOutput
     * @param stage
     * @return 
     */
    public long getStageWaitTime(String dagsimOutput, String stage) {
        String[] stageOutput = filterLines(dagsimOutput.split("\n"), stage);
        return (long)Math.floor(Double.valueOf(extract(stageOutput[1], 4, "\t")));
    }
	
    public int extract(String string, int position)
    {
            String mysubstring = "", savestring = "";


            for (int i = 1; i <= position; i++)
            {
                    mysubstring = string.substring(0, string.indexOf("_"));
                    savestring = mysubstring;
                    string = string.substring(mysubstring.length()+1, string.length());	
            }

            return Integer.valueOf(mysubstring);

        // return Integer.valueOf(extract(string, position, "_"));
    }
        
    /**
     * Returns the n-th substring of s according to the separator. position is 1-based
     * @param s
     * @param position
     * @param separator
     * @return 
     */
    public String extract(String s, int position, String separator) {
        String[] elems = s.split(separator);

        return elems[position - 1];
    }

    public String uploadCsvToTable(String uploadedFileLocation, String filename)
    {
            String cmd = readVar("OPTIMIZE_HOME")+"/optimize "+uploadedFileLocation + " 100 " + filename;
            return _run(cmd);
    }
	
	
	
	
    public String readVar(String property)
    {
            String line, path="";
            int index;
            try {
                InputStream fis = new FileInputStream(System.getenv("HOME").concat("/.ws_properties"));
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);

                while ((line = br.readLine()) != null) {
                    if (line.contains(property))
                    {
                            index = line.indexOf('=');
                            path=line.substring(index+1, line.length());
                            break;
                    }
                }
                br.close();
                    }
            catch (IOException e)
            {
                    e.printStackTrace();
            }
            return(path);
    }
	  
    public String BuildLUA(String resultsPath, String iD, String dsDimension, String coreN, String method, String query)
    {
            String path = resultsPath.concat("/").concat(iD).concat("_").concat(dsDimension).concat("_").concat(coreN).concat("_").concat(method).concat("/").concat(query).concat("/logs/");

            String luaFileName = getFirstFile(getFirstFolder(path));

            return(luaFileName);
    }
    
    public String BuildLUAWithoutMethod(String resultsPath, final String nNodes, final String nCores, final String datasize, String query) {
        // List all files in results path directory
        File [] directories = new File(resultsPath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) 
            {
                return file.isDirectory() && file.getName().startsWith(nNodes + "_" + nCores) && file.getName().endsWith(datasize);
            }
        });
        if (directories.length > 0) {
            String path = directories[0].getPath().concat("/").concat(query).concat("/logs/");
            String luaFileName = getFirstFile(getFirstFolder(path));
            return luaFileName;
        }
        return null;
    }
	 
	  
	  
    public String getFirstFolder(String path)
    {          
            String folderName = path;
            try
            {
                    File folder = new File(path);
                    File[] listOfFiles = folder.listFiles();
                    int i;

                    Arrays.sort(listOfFiles);
                    for (i = 0; i < listOfFiles.length; i++) 
                            if (listOfFiles[i].isDirectory()) break;
                    folderName = listOfFiles[i].getAbsolutePath();
            }
            catch(Exception e)
            {
                    e.printStackTrace();

            }

        return folderName.concat("/");
    }
	  
    public String getFirstFile(String path)
    {          
            // Initialize to worst case: something is wrong with the filename or the path
            String filename = "ERROR: could not find file";
            try
            {
                    File folder = new File(path);
                    File[] listOfFiles = folder.listFiles();
                    int i;

                    for (i = 0; i < listOfFiles.length; i++) 
                            if (
                                            (listOfFiles[i].isFile()) && 
                                            (listOfFiles[i].getName().endsWith(".lua"))
                                            ) break;
                    filename = listOfFiles[i].getAbsolutePath();
            }
            catch(Exception e)
            {
                    e.printStackTrace();

            }

        return filename;
    }
	
    public String _run(String cmd) {    
        try
        {
            final ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", cmd);
                pb.redirectErrorStream();
            final Process process = pb.start();
            final InputStream in = process.getInputStream();
            final byte[] bytes = new byte[1024];
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();


            for (int len; (len = in.read(bytes)) > 0;)
            baos.write(bytes, 0, len);
            process.waitFor();
            return baos.toString(); 

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
        return("Error: could not execute command "+cmd);
    }
	
    // save uploaded file to new location
    public void writeToFile(InputStream uploadedInputStream,
                    String uploadedFileLocation) {

        try {
            OutputStream out = new FileOutputStream(new File(
                            uploadedFileLocation));
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(new File(uploadedFileLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    /**
     * Search in table RUNNING_APPLICATION_TABLE and get corresponding app_id
     * @param appSessId
     * @param dbName
     * @param connection
     * @return
     * @throws SQLException 
     */
    public String retrieveAppId(String appSessId, String dbName, Connection connection) throws SQLException {
        String query_stmt = "SELECT application_id FROM " + dbName + ".RUNNING_APPLICATION_TABLE WHERE application_session_id = '" + appSessId + "'";
        
        ResultSet results = query(dbName, connection, query_stmt);
        if (results != null && results.next()) {
            return results.getString("application_id");
        }
        throw new SQLException("Could not retrieve application ID from database");
    }
    
    /**
     * Search in table RUNNING_APPLICATION_TABLE for submission time
     * @param appSessId
     * @param dbName
     * @param connection
     * @return
     * @throws SQLException 
     */
    public Timestamp retrieveSubmissionTime(String appSessId, String dbName, Connection connection) throws SQLException {
        String get_submission_time_query = "SELECT submission_time FROM " + dbName + ".RUNNING_APPLICATION_TABLE WHERE application_session_id = '"+ appSessId + "'";
            
        ResultSet results = query(dbName, connection, get_submission_time_query); 
        if (results != null && results.next()) {
            return results.getTimestamp("submission_time");
        }
        throw new SQLException("Could not retrieve submission time from database");
    }
    
    /**
     * Finds the best suitable choice for nNodes and nCores if we don't have any directly associated data
     * @param resultsPath
     * @param nNodes
     * @param nCores
     * @param dataset
     * @param method
     * @param appId
     * @return A (possibly null) string of type "<nNodes> <nCores>". If the correct folder is found, then the two values will correspond to the parameters. A null string is returned if the results folder has not been found.
     */
    public String findBestMatch(String resultsPath, String nNodes, String nCores, String dataset, String method, String appId) {
        String path = resultsPath + "/" + nNodes + "_" + nCores + "_" + dataset + "_" + method +"/" + appId;
        File f = new File(path);
        if (!f.exists())
        {
            // Get the list of all the folders
            File [] directories = new File(resultsPath).listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) 
                {
                    return file.isDirectory();
                }
            });

            if (directories == null)	
            {
                System.out.println("Fatal error: no sub-directories have been found in " + resultsPath);
                return null;
            }
            String reply = bestMatch(directories, nNodes, nCores, dataset, method, appId);

            return reply;
        }
        else
            return nNodes + " " + nCores + " ";
    }
    
    /**
     * Search in the lookup table for precomputed values and returns the result
     * @param connection
     * @param dbName
     * @param appId
     * @param nCores
     * @param stage
     * @param dataset
     * @return A ResultSet containing 'stage_end_time' and 'remaining_time' if results are precomputed. Null otherwise
     * @throws SQLException 
     */
    
    public ResultSet lookupDagsimStageEndTime(Connection connection, String dbName, String appId, String nCores, String stage, String dataset) throws SQLException {
        String query = "SELECT val FROM " + dbName + ".PREDICTOR_CACHE_TABLE WHERE is_residual = FALSE AND application_id = '" + appId + "' AND num_cores = " + nCores + " AND stage='" + stage + "' AND dataset_size=" + dataset;
        
        ResultSet results = query(dbName, connection, query);
        if (results != null && results.next()) {
            return results;
        }
        else {
            return null;
        }
    }
    
    public ResultSet lookupDagsimStageRemainingTime(Connection connection, String dbName, String appId, String nCores, String stage, String dataset) throws SQLException {
        String query = "SELECT val FROM " + dbName + ".PREDICTOR_CACHE_TABLE WHERE is_residual = TRUE AND application_id = '" + appId + "' AND num_cores = " + nCores + " AND stage='" + stage + "' AND dataset_size=" + dataset;
        
        ResultSet results = query(dbName, connection, query);
        if (results != null && results.next()) {
            return results;
        }
        else {
            return null;
        }
    }
    
    /**
     * Saves the results in the lookup table
     * @param connection
     * @param dbName
     * @param appId
     * @param nCores
     * @param stage
     * @param dataset
     * @param stage_end_time
     * @param remainingTime
     * @throws SQLException 
     */
    public void saveLookupDagsimStages(Connection connection, String dbName, String appId, String nCores, String stage, String dataset, long stage_end_time, long remainingTime) throws SQLException {
        String save_stage_end = "REPLACE INTO " + dbName + ".PREDICTOR_CACHE_TABLE(application_id, num_cores, stage, dataset_size, val, is_residual) VALUES ('" + appId + "', " + nCores + ", '" + stage + "', " + dataset + ", " + stage_end_time + ", FALSE)";
        String save_remaining_time = "REPLACE INTO " + dbName + ".PREDICTOR_CACHE_TABLE(application_id, num_cores, stage, dataset_size, val, is_residual) VALUES ('" + appId + "', " + nCores + ", '" + stage + "', " + dataset + ", " + remainingTime + ", TRUE)";
        
        insert(dbName, connection, save_stage_end);
        insert(dbName, connection, save_remaining_time);
        connection.commit();
    }
    
    
    /**
     * Retrieve all the stages ids from the dagsim output
     * @param dagsimOutput the output of dagsim run with -s option
     * @return 
     */
    public String[] getAllStages(String dagsimOutput) {
        String[] lines = dagsimOutput.split("\n");
        int num_stages = (lines.length / 4) - 1;
                
        String[] stages = new String[num_stages];
        for (int i = 0; i < num_stages; i++)
            stages[i] = extract(lines[i*4 + 4], 9, "\t");
  
        return stages;
    }
}
