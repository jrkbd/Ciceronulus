package tulliolus.main;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.sql.SQLException;

public class Helper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private String TAG = Helper.class.getCanonicalName();
    private static String DATABASEpath, DATABASE_NAME,FULL_DATABASE_PATH ;
    private SQLiteDatabase database;
    private Context context;
    
    Boolean try2;
    
    File sd, data;
	String sdDBPath = "/Tulliolus/paradigm.db";
	File localDB, sdDB;
	
    public Helper(Context context, String DATABASE_NAME) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.DATABASE_NAME = DATABASE_NAME;
        this.context = context;
        
        try2=false;
        
        if (android.os.Build.VERSION.SDK_INT >= 4.2) {
            this.DATABASEpath = context.getApplicationInfo().dataDir + "/databases/";
        } else {
            this.DATABASEpath = "/data/data/" + context.getPackageName() + "/databases/";
        }
        this.FULL_DATABASE_PATH = DATABASEpath+DATABASE_NAME;

        localDB = new File(DATABASEpath);
        sdDB = new File(sd, sdDBPath);

        sd = Environment.getExternalStorageDirectory();
        data = Environment.getDataDirectory();
        setupDatabase();
        checkSD();
    }


    /**
     * Creates a empty database on the system and rewrites it with the database in assets.
     */
    private void setupDatabase() {
        Log("Attempting to setup database");
        
        if (checkIfDatabaseFileExists()) {    // check if database exists, if not copy local file
            Log("Database exists. Copying from sd.");
            this.getWritableDatabase();
            this.close();     
            try {
            	copyDatabaseFromSDToSystem();        // copy from local file
            } catch (Exception e) {
                Log("COULD NOT READ SD DB FILE");
                e.printStackTrace();
            }
            
        } else {            // create database and repopulate (the earth)
            Log("Database doesn't exist locally. Attempting to copy database from assets");

            this.getWritableDatabase();     // creates an empty database that we will later write over
            this.close();           // close it
            try {
                copyDatabaseFromLocalFileToSystem();        // copy from local file
            } catch (Exception e) {
                Log("COULD NOT READ LOCAL DB FILE");
                e.printStackTrace();
            }
        }

        try {
            openDatabase();
        } catch (SQLException e) {
            Log("Could not open database");
        }

        if (!checkIfDatabaseHasData() && try2==false) {        // check if theres data!
           // Log.d(Helper.class.getCanonicalName(), "DATABASE doesnt have data. Something is wrong. Check database file in assets");
            File dbFile = new File(FULL_DATABASE_PATH);
            dbFile.delete();
            Log.d(TAG, "DB has no data. deleting db. trying again.");
           
            try2=true;
            
            setupDatabase();
        }
    }


    @Override
    public void onCreate(SQLiteDatabase database) {


        Log("Database onCreate() called");
        this.database = database;

    }
    


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(Helper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data"
        );
        db.execSQL("DROP TABLE IF EXISTS " + TBnoun);
        db.execSQL("DROP TABLE IF EXISTS " + TBnoundeclension1);
        db.execSQL("DROP TABLE IF EXISTS " + TBnoundeclension2);
        db.execSQL("DROP TABLE IF EXISTS " + TBnoundeclension3);
        db.execSQL("DROP TABLE IF EXISTS " + TBnoundeclension4);
        db.execSQL("DROP TABLE IF EXISTS " + TBnoundeclension5);
        db.execSQL("DROP TABLE IF EXISTS " + TBverb);
        db.execSQL("DROP TABLE IF EXISTS " + TBverbendactimpfut);
        db.execSQL("DROP TABLE IF EXISTS " + TBverbendactimppres);
        db.execSQL("DROP TABLE IF EXISTS " + TBverbendactindfut);
        db.execSQL("DROP TABLE IF EXISTS " + TBverbendactindimperf);
        db.execSQL("DROP TABLE IF EXISTS " + TBverbendactindpres);
        db.execSQL("DROP TABLE IF EXISTS " + TBverbendactinfperf);
        db.execSQL("DROP TABLE IF EXISTS " + TBverbendactinfpres);

        onCreate(db);
    }


    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     */
    public boolean checkIfDatabaseFileExists() {
        Log.d(Helper.class.getCanonicalName(), "Attempting to open database on " + FULL_DATABASE_PATH);
        File dbFile = new File(FULL_DATABASE_PATH);
        return dbFile.exists();
    }

    public boolean checkIfDatabaseHasData() {
        boolean exists = false;
        Log("checking if database has data");
        Cursor result = null;
        if (database != null) {    // if database object is null, the database doesnt exist
            try {
                result = database.rawQuery("SELECT COUNT(_id) AS countid FROM Noun", null);      // check to see if anything exists in the Term table
                exists = result.moveToFirst();
                if (exists) {
                    exists = result.getInt(0) > 0;
                    Log.d(Helper.class.getCanonicalName(), "number of id rows in Term table returned: " + result.getInt(0));
                }
            } catch (Exception e) {
                Log.d(Helper.class.getCanonicalName(), "data check query failed: must reset data");
                exists = false;
            } finally {
                if (result != null) {
                    result.close();
                }
            }

        }
        // dont close database
        return exists;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     */
    private void copyDatabaseFromLocalFileToSystem() throws IOException {

        //Open your local db as the input stream
        InputStream input = context.getAssets().open(DATABASE_NAME);

        //Open the empty db as the output stream
        OutputStream output = new FileOutputStream(FULL_DATABASE_PATH);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        //Close the streams
        output.flush();
        output.close();
        input.close();
    }
    
    private void copyDatabaseFromSDToSystem() throws IOException {

    	
        try {
        	
        	String dataDBPath = FULL_DATABASE_PATH;
            String sdDBPath = "/Tulliolus/paradigm.db";
            File dataDB = new File(FULL_DATABASE_PATH);
            File sdDB = new File(sd, sdDBPath);
  
                    FileChannel dst = new FileOutputStream(dataDB).getChannel();
                    FileChannel src = new FileInputStream(sdDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    
                  Log.d(TAG, "Database copied from SD");
            }
    
         catch (Exception e) {
        	
        	 Log.d(TAG, "error copying database from SD db:"+e);}
        
     
        
    }
    private void openDatabase() throws SQLException {
        database = SQLiteDatabase.openDatabase(FULL_DATABASE_PATH, null, SQLiteDatabase.OPEN_READWRITE);
        Log.d(Helper.class.getCanonicalName(), "Opened Read/Write database, isOpen:" + database.isOpen());
    }


    /**
     * Just a method to make logging easier
     * @param message
     */
    private void Log(String message) {
        Log.d(Helper.class.getCanonicalName(), message);
    }

    
    public void checkSD(){
            
            File tulliolus = new File(sd, "/Tulliolus/");
            if(!tulliolus.exists())
            	tulliolus.mkdirs();
            else
                Log.d(TAG, "Tulliolus file exists");
    }
    
	public void updateDatabase(){
	
		boolean deleted = sdDB.delete();
		if (deleted = true){
			
			exportDatabase();
			Log.d(TAG, "Database updated");
		}
		else{
			
		Log.d(TAG, "paradigm.db not deleted for update");
		}
	}

	public void exportDatabase() {
		Log.d(TAG, "trying to export db");
	
		
        try {
        	
        	String currentDBPath = "//data//tulliolus.main//databases//paradigm.db";
            String backupDBPath = "/Tulliolus/paradigm.db";
            File currentDB = new File(data, currentDBPath);
            File backupDB = new File(sd, backupDBPath);
  
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
               
            }
    
         catch (Exception e) {
        	
        	 Log.d(TAG, "error exporting db:"+e);}
        
    
}

    // ////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////////
    // //DATABASE CREATE STRINGS
    /*
	 * create table Noun(_id integer primary key autoincrement, Lemma, Gender,
	 * Stem, Declension, Note); create table NounDeclension1(_id integer primary
	 * key autoincrement, Type, NomS, AccS, GenS, DatS, AblS, VocS, NomP, AccP,
	 * GenP, DatP, AblP, VocP); create table NounDeclension2(_id integer primary
	 * key autoincrement, Type, NomS, AccS, GenS, DatS, AblS, VocS, NomP, AccP,
	 * GenP, DatP, AblP, VocP); create table NounDeclension3(_id integer primary
	 * key autoincrement, Type, NomS, AccS, GenS, DatS, AblS, VocS, NomP, AccP,
	 * GenP, DatP, AblP, VocP); create table NounDeclension4(_id integer primary
	 * key autoincrement, Type, NomS, AccS, GenS, DatS, AblS, VocS, NomP, AccP,
	 * GenP, DatP, AblP, VocP); create table NounDeclension5(_id integer primary
	 * key autoincrement, Type, NomS, AccS, GenS, DatS, AblS, VocS, NomP, AccP,
	 * GenP, DatP, AblP, VocP); create table Verb(_id integer primary key
	 * autoincrement, PP1, PP2, PP3, PP4, StemPresent, StemPerfect, StemSupine,
	 * Conjugation, Note); create table VerbEndActImpFut(_id integer primary key
	 * autoincrement, End, Voice, Mood, Tense, Person, Number); create table
	 * VerbEndActImpPres(_id integer primary key autoincrement, End, Voice,
	 * Mood, Tense, Person, Number); create table VerbEndActIndFut(_id integer
	 * primary key autoincrement, End, Voice, Mood, Tense, Person, Number);
	 * create table VerbEndActIndImperf(_id integer primary key autoincrement,
	 * End, Voice, Mood, Tense, Person, Number); create table
	 * VerbEndActIndPres(_id integer primary key autoincrement, End, Voice,
	 * Mood, Tense, Person, Number); create table VerbEndActInfPerf(_id integer
	 * primary key autoincrement, End, Voice, Mood, Tense, Person, Number);
	 * create table VerbEndActInfPres(_id integer primary key autoincrement,
	 * End, Voice, Mood, Tense, Person, Number); create table
	 * VerbEndActSubjImperf(_id integer primary key autoincrement, End, Voice,
	 * Mood, Tense, Person, Number); create table VerbEndActSubjPres(_id integer
	 * primary key autoincrement, End, Voice, Mood, Tense, Person, Number);
	 */

    // /NOUN
    public static final String TBnoun = "Noun";
 
    // //

    // /NOUN DECLENSIONS
    public static final String TBnoundeclension1 = "NounDeclension1";
    public static final String TBnoundeclension2 = "NounDeclension2";
    public static final String TBnoundeclension3 = "NounDeclension3";
    public static final String TBnoundeclension4 = "NounDeclension4";
    public static final String TBnoundeclension5 = "NounDeclension5";
 

    public static final String TBverb = "Verb";
   
    public static final String TBverbendactimpfut = "VerbEndActImpFut";
    public static final String TBverbendactimppres = "VerbEndActImpPres";
    public static final String TBverbendactindfut = "VerbEndActIndFut";
    public static final String TBverbendactindimperf = "VerbEndActIndImperf";
    public static final String TBverbendactindpres = "VerbEndActIndPres";
    public static final String TBverbendactinfperf = "VerbEndActInfPerf";
    public static final String TBverbendactinfpres = "VerbEndActInfPres";
    public static final String TBverbendactsubjimperf = "VerbEndActSubjImperf";
    public static final String TBverbendactsubjpres = "VerbEndActSubjPres";
  
}
