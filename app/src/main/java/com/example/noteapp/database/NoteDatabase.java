package com.example.noteapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.noteapp.dao.NoteDao;
import com.example.noteapp.entities.Note;

@Database(entities = Note.class,version = 1,exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {
    private static NoteDatabase noteDatabase;

    public static NoteDatabase getNoteDatabase(Context context){
        if(noteDatabase == null){
            synchronized (NoteDatabase.class){
                if(noteDatabase == null){
                    noteDatabase = Room.databaseBuilder(context,NoteDatabase.class,"note").build();
                }
            }
        }
        return noteDatabase;
    }

    public abstract NoteDao noteDao();
}
