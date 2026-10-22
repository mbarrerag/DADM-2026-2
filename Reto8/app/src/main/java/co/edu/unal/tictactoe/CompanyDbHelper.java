package co.edu.unal.tictactoe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class CompanyDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "software_directory.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE = "companies";

    public CompanyDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE companies (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, url TEXT, phone TEXT, email TEXT, products TEXT, classification TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS companies");
        onCreate(db);
    }

    public long save(Company company) {
        ContentValues values = valuesFor(company);
        SQLiteDatabase db = getWritableDatabase();
        if (company.id == 0) {
            return db.insert(TABLE, null, values);
        }
        db.update(TABLE, values, "id = ?", new String[]{String.valueOf(company.id)});
        return company.id;
    }

    public void delete(long id) {
        getWritableDatabase().delete(TABLE, "id = ?", new String[]{String.valueOf(id)});
    }

    public List<Company> find(String nameFilter, String classificationFilter) {
        List<Company> companies = new ArrayList<>();
        String selection = "name LIKE ?";
        List<String> args = new ArrayList<>();
        args.add("%" + nameFilter + "%");
        if (!"Todas".equals(classificationFilter)) {
            selection += " AND classification = ?";
            args.add(classificationFilter);
        }
        Cursor cursor = getReadableDatabase().query(TABLE, null, selection, args.toArray(new String[0]), null, null, "name COLLATE NOCASE ASC");
        try {
            while (cursor.moveToNext()) {
                Company company = new Company();
                company.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                company.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                company.url = cursor.getString(cursor.getColumnIndexOrThrow("url"));
                company.phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                company.email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                company.products = cursor.getString(cursor.getColumnIndexOrThrow("products"));
                company.classification = cursor.getString(cursor.getColumnIndexOrThrow("classification"));
                companies.add(company);
            }
        } finally {
            cursor.close();
        }
        return companies;
    }

    private ContentValues valuesFor(Company company) {
        ContentValues values = new ContentValues();
        values.put("name", company.name);
        values.put("url", company.url);
        values.put("phone", company.phone);
        values.put("email", company.email);
        values.put("products", company.products);
        values.put("classification", company.classification);
        return values;
    }
}
