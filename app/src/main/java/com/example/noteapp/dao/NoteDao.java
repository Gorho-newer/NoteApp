package com.example.noteapp.dao;

import android.view.ViewDebug;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.noteapp.entities.Note;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("select * from note order by id desc")
    List<Note> getAllNotes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Delete
    void deleteNote(Note note);
}
