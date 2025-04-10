package com.example.homerent;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.homerent.databinding.FragmentAccountBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private FragmentAccountBinding binding;
    public static final int MY_REQUEST_CODE = 10; // hoặc số nào cũng được
final private ChangeAccountActivity fragmentChangeAccount= new ChangeAccountActivity();
    final private ActivityResultLauncher<Intent> mActivityResultLauncher= registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode()==RESULT_OK){
                        Intent intent= result.getData();
                        if(intent==null){
                            return;
                        }
                        Uri uri=intent.getData();
/*
                        fragmentChangeAccount.setImageUri(uri);
*/
                        try {
                            Bitmap bitmap= MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
/*
                            fragmentChangeAccount.setBitmapImage(bitmap);
*/
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    public void showUserInformation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String name = user.getDisplayName();
        String email = user.getEmail();
        Uri photoUrl = user.getPhotoUrl();

        if (name == null || name.isEmpty()) {
            binding.tvName.setVisibility(View.GONE);
        } else {
            binding.tvName.setVisibility(View.VISIBLE);
            binding.tvName.setText(name);
        }
        binding.tvEmail.setText(email != null ? email : "No email available");

        if (photoUrl != null) {
            Glide.with(this).load(photoUrl).into(binding.imgAvatar);
        } else {
            binding.imgAvatar.setImageResource(R.drawable.ic_ava);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==MY_REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                openGallery();
            }
            else {
                Toast.makeText(this, "Vui Lòng Cấp Quyền", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void openGallery(){
        Intent intent= new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        mActivityResultLauncher.launch(Intent.createChooser(intent,"Select Picture"));

    }
}