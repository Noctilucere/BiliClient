package com.BiliClient.Noctilucere.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.BiliClient.Noctilucere.R;
import com.BiliClient.Noctilucere.model.Reply;
import com.BiliClient.Noctilucere.util.LittleToolsUtil;

public class ReplyCardHolder extends RecyclerView.ViewHolder{
    TextView content,pubdate,tiptext;
    public ReplyCardHolder(@NonNull View itemView) {
        super(itemView);
        content = itemView.findViewById(R.id.content);
        tiptext = itemView.findViewById(R.id.tip);
    }
    public void showReplyCard(Reply replyInfo){
        content.setText(LittleToolsUtil.htmlToString(replyInfo.message));
        if(replyInfo.isDynamic) tiptext.setText("不支持查看动态");
    }
}
