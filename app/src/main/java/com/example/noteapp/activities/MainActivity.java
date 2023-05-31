package com.example.noteapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.noteapp.R;
import com.example.noteapp.adapters.NotesAdapter;
import com.example.noteapp.database.NoteDatabase;
import com.example.noteapp.entities.Note;
import com.example.noteapp.listeners.NotesListeners;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListeners {

    private static final int REQUEST_CODE_ADD_NOTE = 1;
    //used to update note
    private static final int REQUEST_CODE_UPDATE_NOTE = 2;
    private static final int REQUEST_CODE_SHOW_NOTES = 3;
    private static final int REQUEST_CODE_SELECT_IMAGE = 4;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 5;

    private RecyclerView noteRecyclerView;
    private List<Note> noteList;
    private NotesAdapter noteAdapter;

    //当前被选中的笔记
    private int clickedNotePosition = -1;

    private AlertDialog dialogAddURL;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(getApplicationContext(), CreateNoteActivity.class),REQUEST_CODE_ADD_NOTE);
            }
        });

        //实例化recyclerView
        noteRecyclerView = findViewById(R.id.notesRecyclerView);
        noteRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));

        noteList = new ArrayList<>();
        noteAdapter = new NotesAdapter(noteList,this);
        noteRecyclerView.setAdapter(noteAdapter);

        getNotes(REQUEST_CODE_SHOW_NOTES,false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                noteAdapter.cancelTimer();
                Log.d("Timer","onTextChanged");
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(noteList.size() != 0){
                    noteAdapter.searchNotes(editable.toString());
                }
            }
        });

        //快捷按钮绑定
        findViewById(R.id.imageAddNote).setOnClickListener(v -> {
            startActivityForResult(new Intent(getApplicationContext(), CreateNoteActivity.class),REQUEST_CODE_ADD_NOTE);
        });

        findViewById(R.id.imageAddImage).setOnClickListener(v -> {
            if(ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //向系统请求权限
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
            }else{
                selectImage();
            }
        });

        findViewById(R.id.imageAddWebLink).setOnClickListener(v -> {
            showAddUrlDialog();
        });

        //设置动画背景
        ConstraintLayout mainLayout = findViewById(R.id.layout_main);
        AnimationDrawable animationDrawable = (AnimationDrawable) mainLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2500);
        animationDrawable.setExitFadeDuration(5000);

        animationDrawable.start();
    }

    private void showAddUrlDialog(){
        if(dialogAddURL == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_add_url,(ViewGroup)findViewById(R.id.layoutAddUrlContainer));

            builder.setView(view);
            dialogAddURL = builder.create();
            if(dialogAddURL.getWindow() != null){
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputUrl = view.findViewById(R.id.inputURL);
            inputUrl.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(v -> {
                if(inputUrl.getText().toString().trim().isEmpty()){
                    Toast.makeText(MainActivity.this,"Enter URL",Toast.LENGTH_SHORT).show();
                }else if(!Patterns.WEB_URL.matcher(inputUrl.getText().toString()).matches()){
                    Toast.makeText(MainActivity.this,"Enter valid URL",Toast.LENGTH_SHORT).show();
                }else{
                    dialogAddURL.dismiss();
                    Intent intent = new Intent(getApplicationContext(),CreateNoteActivity.class);
                    intent.putExtra("isFromQuickActions",true);
                    intent.putExtra("quickActionType","URL");
                    intent.putExtra("URL",inputUrl.getText().toString());
                    startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> {
                dialogAddURL.dismiss();
            });
        }

        dialogAddURL.show();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent,REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            }else {
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getNotes(final int requestCode, final boolean isNoteDeleted){
        class GetNotesTask extends AsyncTask<Void,Void, List<Note>>{

            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                if(requestCode == REQUEST_CODE_SHOW_NOTES){
                    noteList.addAll(notes);
                    noteAdapter.notifyDataSetChanged();
                }else if(requestCode == REQUEST_CODE_ADD_NOTE){
                    noteList.add(0,notes.get(0));
                    noteAdapter.notifyItemInserted(0);
                    noteRecyclerView.smoothScrollToPosition(0);
                }else if(requestCode == REQUEST_CODE_UPDATE_NOTE){
                    noteList.remove(clickedNotePosition);
                    System.out.println("移除的序号:"+clickedNotePosition);
                    if(isNoteDeleted){
                        System.out.println("列表移出更新");
                        noteAdapter.notifyItemRemoved(clickedNotePosition);
                    }else{
                        System.out.println("列表修改更新");
                        noteList.add(clickedNotePosition,notes.get(clickedNotePosition));
                        noteAdapter.notifyItemChanged(clickedNotePosition);
                    }
                }
            }
        }

        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK){
            getNotes(REQUEST_CODE_ADD_NOTE,false);
        }else if(requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK){
            if(data != null){
                getNotes(REQUEST_CODE_UPDATE_NOTE,data.getBooleanExtra("isNoteDeleted",false));
            }
        }else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if(data != null){
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null){
                    try{
                        String selectedImagePath = getPathFromUri(selectedImageUri);
                        Intent intent = new Intent(getApplicationContext(),CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions",true);
                        intent.putExtra("quickActionType","image");
                        intent.putExtra("imagePath",selectedImagePath);
                        startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);
                    }catch (Exception e){
                        Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        //当前被选中的序号
        clickedNotePosition = position;
        System.out.println("当前选中的序号:"+clickedNotePosition);
        Intent intent = new Intent(getApplicationContext(),CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate",true);
        intent.putExtra("note",note);
        startActivityForResult(intent,REQUEST_CODE_UPDATE_NOTE);
    }

    private String getPathFromUri(Uri contentUri){
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri,null,null,null,null);
        if(cursor == null){
            filePath = contentUri.getPath();
        }else{
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }

        return filePath;
    }
}