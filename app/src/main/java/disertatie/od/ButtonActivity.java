package disertatie.od;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ButtonActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        Button buttonRT = findViewById(R.id.buttonRT);
        Button buttonImage = findViewById(R.id.buttonImage);

        buttonRT.setOnClickListener(this);
        buttonImage.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonRT:
                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                startActivity(intent);
                break;
            case R.id.buttonImage:
                Intent intent1 = new Intent(getApplicationContext(), ImageDetectorActivity.class);
                startActivity(intent1);
                break;
        }
    }
}
