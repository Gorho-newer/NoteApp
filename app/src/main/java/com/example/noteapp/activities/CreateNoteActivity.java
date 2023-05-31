package com.example.noteapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.noteapp.R;
import com.example.noteapp.database.NoteDatabase;
import com.example.noteapp.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText inputNoteTitle,inputNoteSubtitle,inputNoteText;
    private TextView textDateTime;
    private ImageView imageNote;
    private TextView textWebURL;
    private LinearLayout layoutWebURL;

    //颜色标签
    private View viewSubtitleIndicator;
    private String selectedNoteColor;

    private String selectedImagePath;

    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteNote;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    //当前被查看对象
    private Note alreadyAvailableNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(view -> onBackPressed());

        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inputNoteText = findViewById(R.id.inputNoteText);
        textDateTime = findViewById(R.id.textDateTime);

        imageNote = findViewById(R.id.imageNote);
        textWebURL = findViewById(R.id.textWebURL);
        layoutWebURL = findViewById(R.id.layoutWebURL);

        textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date())
        );

        ImageView imageDone = findViewById(R.id.imageSave);
        imageDone.setOnClickListener(v -> saveNote());

        //default note color
        selectedNoteColor = "#333333";
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        selectedImagePath = "";

        //设置当前要查看笔记
        if(getIntent().getBooleanExtra("isViewOrUpdate",false)){
            alreadyAvailableNote = (Note)getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        //初始化删除webURL按钮
        findViewById(R.id.imageRemoveWebURL).setOnClickListener(v -> {
            textWebURL.setText(null);
            layoutWebURL.setVisibility(View.GONE);
        });

        findViewById(R.id.imageRemoveImage).setOnClickListener(v -> {
            imageNote.setImageBitmap(null);
            imageNote.setVisibility(View.GONE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
            selectedImagePath = "";
        });

        if(getIntent().getBooleanExtra("isFromQuickActions",false)){
            String type = getIntent().getStringExtra("quickActionType");
            if(type != null){
                if(type.equals("image")){
                    selectedImagePath = getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                }else if(type.equals("URL")){
                    //展示url组件
                    textWebURL.setText(getIntent().getStringExtra("URL"));
                    layoutWebURL.setVisibility(View.VISIBLE);
                }
            }
        }

        //初始化底部弹窗
        initMiscellaneous();
        //设置副标题标签颜色
        setSubtitleIndicatorColor();
    }

    private void setViewOrUpdateNote(){
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        textDateTime.setText(alreadyAvailableNote.getDateTime());

        if(alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()){
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if(alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()){
            textWebURL.setText(alreadyAvailableNote.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);
        }
    }

    private void saveNote(){
        if(inputNoteTitle.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Note title can't be empty!",Toast.LENGTH_SHORT).show();
            return;
        }else if(inputNoteSubtitle.getText().toString().trim().isEmpty()
                && inputNoteText.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Note can't be empty!",Toast.LENGTH_SHORT).show();
            return;
        }

        final Note note = new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setSubtitle(inputNoteSubtitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDateTime(textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        /*此处weblink的设置有点搞不懂*/
        if(layoutWebURL.getVisibility() == View.VISIBLE){
            note.setWebLink(textWebURL.getText().toString());
        }

        if(alreadyAvailableNote != null){
            note.setId(alreadyAvailableNote.getId());
        }

        //内部类
        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void,Void,Void>{
            public SaveNoteTask(){
                super();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao().insertNote(note);
                Log.d("My Notes",note.toString());
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                Intent intent = new Intent();
                setResult(RESULT_OK,intent);
                //隐藏软键盘
                InputMethodManager imm = (InputMethodManager) CreateNoteActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(CreateNoteActivity.this.getWindow().getDecorView().getWindowToken(),0);
                finish();
            }
        }

        //实例化异步类
        new SaveNoteTask().execute();
    }

    private void initMiscellaneous(){
        final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);

        layoutMiscellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(v -> {
            if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED){
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }else{
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        //单击view改变颜色
        final ImageView item1 = layoutMiscellaneous.findViewById(R.id.imageColor1);
        final ImageView item2 = layoutMiscellaneous.findViewById(R.id.imageColor2);
        final ImageView item3 = layoutMiscellaneous.findViewById(R.id.imageColor3);
        final ImageView item4 = layoutMiscellaneous.findViewById(R.id.imageColor4);
        final ImageView item5 = layoutMiscellaneous.findViewById(R.id.imageColor5);

        layoutMiscellaneous.findViewById(R.id.viewColor1).setOnClickListener(v -> {
            selectedNoteColor = "#333333";
            item1.setImageResource(R.drawable.ic_done);
            item2.setImageResource(0);
            item3.setImageResource(0);
            item4.setImageResource(0);
            item5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor2).setOnClickListener(v -> {
            selectedNoteColor = "#FDBE3B";
            item2.setImageResource(R.drawable.ic_done);
            item1.setImageResource(0);
            item3.setImageResource(0);
            item4.setImageResource(0);
            item5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor3).setOnClickListener(v -> {
            selectedNoteColor = "#FF4842";
            item3.setImageResource(R.drawable.ic_done);
            item2.setImageResource(0);
            item1.setImageResource(0);
            item4.setImageResource(0);
            item5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor4).setOnClickListener(v -> {
            selectedNoteColor = "#3A52FC";
            item4.setImageResource(R.drawable.ic_done);
            item1.setImageResource(0);
            item2.setImageResource(0);
            item3.setImageResource(0);
            item5.setImageResource(0);
            setSubtitleIndicatorColor();
        });
        layoutMiscellaneous.findViewById(R.id.viewColor5).setOnClickListener(v -> {
            selectedNoteColor = "#000000";
            item5.setImageResource(R.drawable.ic_done);
            item2.setImageResource(0);
            item1.setImageResource(0);
            item4.setImageResource(0);
            item3.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        //进入笔记时更新颜色
        if(alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()){
            //进入判断了
            Log.d("ColorTest","进来了");
            System.out.println("初始化底部栏了捏");
            switch (alreadyAvailableNote.getColor()){
                case "#FDBE3B":
                    layoutMiscellaneous.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#FF4842":
                    layoutMiscellaneous.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#3A52FC":
                    layoutMiscellaneous.findViewById(R.id.viewColor4).performClick();
                    Log.d("ColorTest","更新了");
                    break;
                case "#000000":
                    layoutMiscellaneous.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }

        layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(v -> {
            //收起底部栏
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            //如果自身清单权限不符合批准
            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(CreateNoteActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
            }else{
                selectImage();
            }
        });

        layoutMiscellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(v -> {
            //收起底部栏
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddUrlDialog();
        });

        //已经有笔记了才能删除
        if(alreadyAvailableNote != null){
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });
        }
    }

    private void showDeleteNoteDialog(){
        if(dialogDeleteNote == null){
            //弹窗出现的上下文
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            //找到弹窗内要显示的界面
            View view = LayoutInflater.from(CreateNoteActivity.this).inflate(
                    R.layout.layout_delete_note,
                    (ViewGroup)findViewById(R.id.layoutDeleteNoteContainer));
            builder.setView(view);
            dialogDeleteNote = builder.create();

            if(dialogDeleteNote.getWindow() != null){
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.textDeleteNote).setOnClickListener(v -> {
                class DeleteAsyncTask extends AsyncTask<Void,Void,Void>{
                    @Override
                    protected Void doInBackground(Void... voids) {
                        System.out.println(NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao().getAllNotes());
                        NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao()
                                .deleteNote(alreadyAvailableNote);
                        System.out.println(NoteDatabase.getNoteDatabase(getApplicationContext()).noteDao().getAllNotes());
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        super.onPostExecute(unused);
                        Intent intent = new Intent();
                        intent.putExtra("isNoteDeleted",true);
                        setResult(RESULT_OK,intent);
                        System.out.println(intent);
                        System.out.println(intent.getExtras().get("isNoteDeleted"));
                        finish();
                    }
                }

                new DeleteAsyncTask().execute();
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> {
                dialogDeleteNote.dismiss();
            });
        }
        dialogDeleteNote.show();
    }

    /**
     * 从系统相册选择图片
     */
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
            selectImage();
        }else{
            Log.d("Permission check","没有同意权限");
            Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if(data != null){
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null){
                    try{
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        //拿到图片后，立刻渲染删除按钮
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                        selectedImagePath = getPathFromUri(selectedImageUri);
                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
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

    private void setSubtitleIndicatorColor(){
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private void showAddUrlDialog(){
        if(dialogAddURL == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
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
                    Toast.makeText(CreateNoteActivity.this,"Enter URL",Toast.LENGTH_SHORT).show();
                }else if(!Patterns.WEB_URL.matcher(inputUrl.getText().toString()).matches()){
                    Toast.makeText(CreateNoteActivity.this,"Enter valid URL",Toast.LENGTH_SHORT).show();
                }else{
                    textWebURL.setText(inputUrl.getText().toString());
                    layoutWebURL.setVisibility(View.VISIBLE);
                    dialogAddURL.dismiss();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> {
                dialogAddURL.dismiss();
            });
        }

        dialogAddURL.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(dialogDeleteNote != null){
            dialogDeleteNote.dismiss();
        }
        if(dialogAddURL != null){
            dialogAddURL.dismiss();
        }
    }
}