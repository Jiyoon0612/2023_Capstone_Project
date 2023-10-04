package com.example.capstone2023;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class spell_check extends AppCompatActivity {

    // 교정 내역 저장하는 변수들
    int len;
    JSONObject[] jsonObjects = new JSONObject[len];
    int[] start, end, correctMethod;
    String[] help, orgStr, candWord, candWord_one;
    int start_flag, no_err_flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spell_check);
        NukeSSLCerts.nuke();
        Intent getIntent = getIntent();

        // 변수 생성
        LinearLayout list_layout = findViewById(R.id.list_layout);  // 오류 내역 layout
        TextView before_text = findViewById(R.id.before_text);      // 검사 전 text
        TextView after_text = findViewById(R.id.after_text);        // 검사 후 text
        TextView list_text = findViewById(R.id.list_text);          // 오류 내역 text
        Button home_btn = findViewById(R.id.home_btn);              // 홈 버튼
        Button reset_btn = findViewById(R.id.reset_btn);            // 내용 변경 버튼
        Button start_btn = findViewById(R.id.start_btn);            // 검사 시작 버튼
        Button copy_btn = findViewById(R.id.copy_btn);              // 복사하기 버튼
        Button list_btn = findViewById(R.id.list_btn);              // 오류 내역 창 열기 버튼
        Button close_btn = findViewById(R.id.close_btn);            // 오류 내역 창 닫기 버튼

        start_flag = 0;                            // 검사 시작 버튼을 안누름
        no_err_flag = 0;                           // 오류가 있음
        list_layout.setVisibility(View.INVISIBLE); // 오류 내역 창 안보이게

        // 이미지 속 text를 before_text에 저장하는 코드
        String before = getIntent.getStringExtra("before_text");
        before_text.setText(before);

        // 홈 버튼 눌렀을 때
        home_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        // 내용 변경 버튼 눌렀을 때
        reset_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), picture.class);
                startActivity(intent);
            }
        });

        // 검사 시작 버튼 눌렀을 때
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start_flag = 1; // 검사 시작 버튼 누름
                // 변수 생성
                String before_text_str = before_text.getText().toString();
                String url = "http://164.125.7.61/speller/results";
                before_text.setText(before_text_str);

                // 서버 통신
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("text1", before_text_str); // 검사 전 text 전송

                // 전송 url 및 data 파싱
                String dataParse = "";
                String getUrl = "";
                dataParse = String.valueOf(params.toString());
                dataParse = dataParse.replaceAll("[{]","");
                dataParse = dataParse.replaceAll("[}]","");
                dataParse = dataParse.replaceAll("[,]","&");
                getUrl = url + "?" + dataParse;

                // response 객체 생성
                StringRequest request = new StringRequest(Request.Method.POST, getUrl,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    // response는 html 형식이므로, html 속 json 가져오기
                                    String jsonString = response.toString().substring(response.toString().indexOf("data = ["), response.toString().lastIndexOf("];") + 1);
                                    String front = "data = [{\"str\":\"" + before_text_str + "\",\"errInfo\":";
                                    String back = ",\"idx\":0}]";
                                    jsonString = jsonString.replace(front, "").replace(back, ""); // json 값을 가진 string

                                    // 값을 저장할 변수 생성
                                    JSONArray jsonArray = new JSONArray(jsonString);    // json 값을 가진 string을 jsonArray로 저장
                                    len = jsonArray.length();                           // jsonArray의 길이
                                    JSONObject[] jsonObjects = new JSONObject[len];     // jsonArray의 크기만큼의 jsonObject 생성
                                    start = new int[len];                               // 수정할 단어의 시작 위치
                                    end = new int[len];                                 // 수정할 단어의 끝 위치
                                    correctMethod = new int[len];                       // 수정할 단어의 형식
                                    help = new String[len];                             // 도움말
                                    orgStr = new String[len];                           // 입력 내용
                                    candWord = new String[len];                         // 대치어
                                    candWord_one = new String[len];                     // 대치어

                                    // 개별 값 저장
                                    for (int i = 0; i < len; i++) {
                                        jsonObjects[i] = jsonArray.getJSONObject(i);
                                        start[i] = jsonObjects[i].getInt("start");
                                        end[i] = jsonObjects[i].getInt("end");
                                        correctMethod[i] = jsonObjects[i].getInt("correctMethod");
                                        help[i] = jsonObjects[i].getString("help");
                                        help[i] = help[i].replace("<br/>", "\n").replace("<br/><br/>", "\n\n");
                                        help[i] = help[i].replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&apos;", "\'");
                                        help[i] = help[i].replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "\'").replace("&#035;", "#");
                                        orgStr[i] = jsonObjects[i].getString("orgStr");
                                        candWord[i] = jsonObjects[i].getString("candWord");
                                        // 대체어가 2개 이상일 때, 첫 번째 값을 저장하도록
                                        if (candWord[i].contains("|")) {
                                            String[] cW_split = candWord[i].split("\\|");
                                            candWord_one[i] = cW_split[0];
                                            int split_len = cW_split.length;
                                            candWord[i] = "";
                                            for (int j = 0; j < split_len; j++) {
                                                candWord[i] += cW_split[j];
                                                if (j + 1 != split_len)
                                                    candWord[i] += " | ";
                                            }
                                        } else {  candWord_one[i] = candWord[i]; }
                                    }
                                    // 맞춤법 교정
                                    int plus = 0;                       // 길이 변화를 알기 위한 변수
                                    int[] start_plus = new int[len];    // 검사 후 색을 변환하기 위하여 필요한 변수
                                    String new_text = before_text_str;  // 변환 값을 저장할 string 변수
                                    for (int i = 0; i < len; i++) {
                                        start_plus[i] = start[i] + plus;
                                        new_text = new_text.substring(0, start_plus[i]) + candWord_one[i] + new_text.substring(end[i] + plus); // 검사 전 text를 한 단어씩 교정
                                        plus += candWord_one[i].length() - (end[i] - start[i]);
                                    }
                                    SpannableString spannableString = new SpannableString(new_text); // 글자색 변환을 위한 변수
                                    for (int i = 0; i < len; i++) {
                                        switch (correctMethod[i]) { // correctMethod에 따른 교정 단어 색상 지정
                                            case 1: //보라
                                            case 5:
                                                spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.blue)), start_plus[i], start_plus[i] + candWord_one[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                break;
                                            case 2: //빨강
                                                spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.red)), start_plus[i], start_plus[i] + candWord_one[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                break;
                                            case 3: //파랑
                                                spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.purple)), start_plus[i], start_plus[i] + candWord_one[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                break;
                                            case 4: //초록
                                                spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.green)), start_plus[i], start_plus[i] + candWord_one[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                break;
                                        }
                                    }
                                    after_text.setText(spannableString);     // after_text을 교정 된 text로 저장
                                } catch (JSONException|StringIndexOutOfBoundsException  e) {
                                    after_text.setText("\n\n  맞춤법과 문법 오류를 찾지 못했습니다.\n\n 기술적 한계로 찾지 못한 맞춤법 오류나 문법 오류가 있을 수 있습니다.");
                                    no_err_flag = 1;                        // 오류가 없음을 알림
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("", "\nresponse fail");   // 연결 실패
                            }
                        });
                request.setShouldCache(false);
                queue.add(request);
            }
        });

        // 전체 복사 버튼 눌렀을 때
        copy_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = after_text.getText().toString().trim();   // 검사 후 text를 string으로 저장
                if (!text.isEmpty()) {
                    // TextView의 텍스트를 가져와서 클립보드에 복사하기
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("text", text);
                    clipboardManager.setPrimaryClip(clipData);
                    // 복사 성공 토스트메시지 노출
                    Toast.makeText(getApplicationContext(), "복사되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    // 텍스트가 비어있는 경우 토스트 메시지 노출
                    Toast.makeText(getApplicationContext(), "복사할 텍스트가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 오류 내역 창 열기 버튼
        list_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 검사 시작 버튼을 누르지 않았다면
                if(start_flag == 0) { Toast.makeText(getApplicationContext(), "검사 시작을 한 후 눌러주세요.", Toast.LENGTH_SHORT).show(); }
                // 검사 시작 버튼을 눌렀다면
                else {
                    list_layout.setVisibility(View.VISIBLE); // 오류 내역 창 보이게

                    if(no_err_flag == 1){ list_text.setText("\n\n  수정할 내용이 없습니다."); } // 오류가 없을 때
                    else {  // 오류가 있을 때
                        String str = "";
                        int[] start_orgstr = new int[len];
                        int[] end_orgstr = new int[len];
                        int[] start_help = new int[len];
                        int[] end_help = new int[len];
                        int[] start_line = new int[len];
                        int[] end_line = new int[len];
                        for (int i = 0; i < len; i++) {
                            str += "입력 내용 : ";
                            start_orgstr[i] = str.length();
                            str += orgStr[i];
                            end_orgstr[i] = str.length();
                            str += "\n" + "     대치어 : " + candWord[i] + "\n" + "     도움말 : ";
                            start_help[i] = str.length();
                            str += help[i] + "\n\n";
                            end_help[i] = str.length();
                            if (i + 1 != len) {
                                start_line[i] = str.length();
                                str += "----------------------------------------------------------------------------\n\n";
                                end_line[i] = str.length();
                            } else {
                                start_line[i] = 0;
                                end_line[i] = 0;
                            }
                        }
                        SpannableString spannableString = new SpannableString(str); // 글자색 변환을 위한 변수
                        for (int i = 0; i < len; i++) {
                            switch (correctMethod[i]) { // correctMethod에 따른 교정 단어 색상 지정
                                case 1: //보라
                                case 5:
                                    spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.blue)), start_orgstr[i], end_orgstr[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    break;
                                case 2: //빨강
                                    spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.red)), start_orgstr[i], end_orgstr[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    break;
                                case 3: //파랑
                                    spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.purple)), start_orgstr[i], end_orgstr[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    break;
                                case 4: //초록
                                    spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.green)), start_orgstr[i], end_orgstr[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    break;//보라
                            }
                            spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.gray)), start_help[i], end_help[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            if (start_line[i] != 0)
                                spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(spell_check.this, R.color.lightgray)), start_line[i], end_line[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        list_text.setText(spannableString);
                    }
                }
            }
        });

        // 오류 내역 창 닫기 버튼
        close_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list_layout.setVisibility(View.INVISIBLE); // 오류 내역 창 안보이게
            }
        });
    }
}
