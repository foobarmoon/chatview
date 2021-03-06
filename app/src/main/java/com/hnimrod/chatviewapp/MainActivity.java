package com.hnimrod.chatviewapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hnimrod.chatview.ChatView;
import com.hnimrod.chatviewapp.api.DialogueClient;
import com.hnimrod.chatviewapp.model.DialogueRepository;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;


public class MainActivity extends AppCompatActivity {
    private static final String TALKAPI_WEBSITE_URL = "https://a3rt.recruit-tech.co.jp/product/talkAPI/";
    private static final String YOUR_IMAGE_URL = "https://avatars0.githubusercontent.com/u/7565281?v=4&s=160";

    @BindView(R.id.layout_chatview) ChatView chatView;
    @BindView(R.id.edit_text) EditText editText;
    @BindView(R.id.send_button) Button sendButton;

    private CompositeDisposable disposable;
    private Unbinder unbinder;
    private boolean showDocomoMessageDone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        unbinder = ButterKnife.bind(this);
        setupSendButton();

        if (savedInstanceState != null) {
            restoreViewState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        unsubscribe();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveViewState();
        super.onSaveInstanceState(outState);
    }

    @OnClick(R.id.send_button)
    void onClickSendButton(View view) {
        if (TextUtils.isEmpty(editText.getText())) {
            return;
        }

        if (!canUseTalkApi() && !showDocomoMessageDone) {
            showSuggestionUseDocomoApiDialog();
            showDocomoMessageDone = true;
        }

        String message = editText.getText().toString();
        editText.setText("");

        int idx = sendMessage(message); // 右(自分)のメッセージ表示
        replyMessage(message, idx); // 左(相手)のメッセージ表示
    }

    private void subscribe(Disposable disposable) {
        if (this.disposable == null) {
            this.disposable = new CompositeDisposable();
        }
        this.disposable.add(disposable);
    }

    private void unsubscribe() {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
    }

    private void restoreViewState() {
        chatView.addAll(DialogueRepository.getInstance().getList());
    }

    private void saveViewState() {
        DialogueRepository repository = DialogueRepository.getInstance();
        repository.clear();
        repository.addAll(chatView.getAll());
    }

    private void setupSendButton() {
        editText.setOnEditorActionListener((TextView textView, int i, KeyEvent keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_SEND) {
                sendButton.callOnClick();
            }
            return true;
        });
    }

    private int sendMessage(@NonNull String message) {
        return chatView.addRightMessage(message);
    }

    private void replyMessage(@NonNull String sendMessage, int sendIdx) {
        Single<String> replySingle = canUseTalkApi() ? DialogueClient.getReply(sendMessage) :
                Single.timer(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).flatMap(done -> Single.just(sendMessage));

        Disposable disposable = replySingle.subscribe(reply -> {
                    if (chatView.getItem(sendIdx) != null) {
                        // 既読に変更
                        chatView.getItem(sendIdx).setReadDone();
                    }
                    chatView.addLeftMessage(reply, YOUR_IMAGE_URL);
                },
                error -> Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_LONG).show());
        subscribe(disposable);
    }

    private boolean canUseTalkApi() {
        return !TextUtils.isEmpty(DialogueClient.API_KEY);
    }

    private void showSuggestionUseDocomoApiDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(R.string.talkapi_suggest_message)
                .setPositiveButton(R.string.ok, (dialog, ok) -> dialog.dismiss())
                .setNegativeButton(R.string.goto_talkapi_site, (dialog, which) -> {
                    Uri uri = Uri.parse(TALKAPI_WEBSITE_URL);
                    Intent i = new Intent(Intent.ACTION_VIEW,uri);
                    startActivity(i);
                })
                .create()
                .show();
    }

}
