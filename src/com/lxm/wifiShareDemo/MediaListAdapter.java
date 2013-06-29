package com.lxm.wifiShareDemo;

import android.content.Context;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 音频媒体数据列表Adapter
 * @author simon.L
 */
public class MediaListAdapter extends BaseAdapter implements DataTransferListener {

    private static final String LOG_TAG = "MediaListAdapter";
    private List<TransmittableMediaItem> mMediaItems = new ArrayList<TransmittableMediaItem>();
    private HashMap<Long, TransmittableMediaItem> mMediaMap = new HashMap<Long, TransmittableMediaItem>();
    private boolean mIsSender;
    private Context mContext;

    public MediaListAdapter(Context context, List<MediaItem> items, boolean isSender) {
        mIsSender = isSender;
        mContext = context;

        swap(items);
    }

    public void add(MediaItem item) {
        if (item != null) {
            TransmittableMediaItem transmittableMediaItem = new TransmittableMediaItem(item, 0, TransmitState.TRANSMIT_IDLE);
            mMediaItems.add(transmittableMediaItem);
            mMediaMap.put(item.mId, transmittableMediaItem);
        }
    }

    public void remove(MediaItem item) {
        if (item != null) {
            mMediaMap.remove(item.mId);
            for (TransmittableMediaItem transmittableMediaItem : mMediaItems) {
                if (transmittableMediaItem.mMediaItem.mId == item.mId) {
                    mMediaItems.remove(transmittableMediaItem);
                    break;
                }
            }
        }
    }

    public void swap(List<MediaItem> items) {
        if (items != null) {
            mMediaItems.clear();
            mMediaMap.clear();
            for (MediaItem item : items) {
                TransmittableMediaItem transmitItem = new TransmittableMediaItem(item, 0, TransmitState.TRANSMIT_IDLE);
                mMediaItems.add(transmitItem);
                mMediaMap.put(item.mId, transmitItem);
            }
        } else {
            mMediaItems.clear();
            mMediaMap.clear();
        }
    }

    @Override
    public int getCount() {
        if (mMediaItems != null) {
            return mMediaItems.size();
        }
        return 0;
    }

    @Override
    public TransmittableMediaItem getItem(int position) {
        if (mMediaItems != null) {
            return mMediaItems.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.transmittable_media_list_item, null);
            ViewHolder holder = new ViewHolder((ProgressBar)convertView.findViewById(R.id.translate_progress)
                , (ImageView)convertView.findViewById(R.id.image_state)
                , (Button)convertView.findViewById(R.id.button_send)
                , (ImageView)convertView.findViewById(R.id.image_delete)
                , (TextView)convertView.findViewById(R.id.text_title)
                , (TextView)convertView.findViewById(R.id.text_detail));
            convertView.setTag(holder);
        }

        bindView(position, convertView);
        return convertView;
    }

    private void bindView(int position, View view) {
        ViewHolder holder = (ViewHolder)view.getTag();
        TransmittableMediaItem transmitMediaItem = mMediaItems.get(position);
        holder.mTitle.setText(transmitMediaItem.mMediaItem.mTitle);
        holder.mSendButton.setText(getSendButtonText(transmitMediaItem.mState));
        holder.mSendButton.setTag(transmitMediaItem);
        holder.mSendButton.setOnClickListener(mSendButtonEvent);
        holder.mProgress.setMax(TransmittableMediaItem.MAX_PROGRESS);
        holder.mProgress.setProgress(transmitMediaItem.mProgress);
        holder.mSendButton.setEnabled(transmitMediaItem.mState == TransmitState.TRANSMIT_IDLE);

        String extension = Utils.getFileExtension(transmitMediaItem.mMediaItem.mData);
        String sizeString = Formatter.formatFileSize(mContext, transmitMediaItem.mMediaItem.mSize);
        holder.mDetail.setText(extension + " " + sizeString);

        if (mIsSender) {
            holder.mSendButton.setVisibility(View.VISIBLE);
            holder.mStateView.setVisibility(View.GONE);
            holder.mDeleteView.setVisibility(View.GONE);
        } else {
            holder.mSendButton.setVisibility(View.GONE);
            holder.mStateView.setVisibility(View.VISIBLE);
            holder.mDeleteView.setVisibility(View.VISIBLE);

            holder.mStateView.setImageResource(transmitMediaItem.mState != TransmitState.TRANSMIT_FINISHED
                    ? R.drawable.icon_downloading : R.drawable.icon_success);

            holder.mDeleteView.setImageResource(R.drawable.icon_delete);
            holder.mDeleteView.setTag(transmitMediaItem);
            holder.mDeleteView.setOnClickListener(mDeleteClickEvent);
        }
    }

    private OnSendClickListener mSendClickListener;

    public void setSendListener(OnSendClickListener listener) {
        mSendClickListener = listener;
    }
    private View.OnClickListener mSendButtonEvent = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TransmittableMediaItem item = (TransmittableMediaItem)v.getTag();
            if (mSendClickListener != null && item.mState == TransmitState.TRANSMIT_IDLE) {
                mSendClickListener.onSendEvent(item);
            }
        }
    };

    @Override
    public void onBegin(MediaItem mediaItem) {
        if (!mIsSender) {
            add(mediaItem);
        }

        TransmittableMediaItem transmittableMediaItem = mMediaMap.get(mediaItem.mId);
        if (transmittableMediaItem != null) {
            transmittableMediaItem.mState = TransmitState.TRANSMITTING;
            transmittableMediaItem.mProgress = 0;

            notifyDataSetChanged();
        }
    }

    @Override
    public void onProgress(MediaItem mediaItem, long transferSize, long totalSize) {
        TransmittableMediaItem transmittableMediaItem = mMediaMap.get(mediaItem.mId);
        if (transmittableMediaItem != null) {
            transmittableMediaItem.mState = TransmitState.TRANSMITTING;
            transmittableMediaItem.mProgress = (int)(transferSize * TransmittableMediaItem.MAX_PROGRESS / totalSize);
            notifyDataSetChanged();
        }
    }

    @Override
    public void onEnd(MediaItem mediaItem) {
        TransmittableMediaItem transmittableMediaItem = mMediaMap.get(mediaItem.mId);
        if (transmittableMediaItem != null) {
            transmittableMediaItem.mState = TransmitState.TRANSMIT_FINISHED;
            transmittableMediaItem.mProgress = TransmittableMediaItem.MAX_PROGRESS;
            notifyDataSetChanged();
        }
    }

    public interface OnSendClickListener {
        public void onSendEvent(TransmittableMediaItem item);
    }

    private View.OnClickListener mDeleteClickEvent = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TransmittableMediaItem item = (TransmittableMediaItem)v.getTag();
        }
    };

    private String getSendButtonText(TransmitState state) {
        switch (state) {
            default:
            case TRANSMIT_IDLE:
                return mIsSender ? "发送" : "接收";
            case TRANSMITTING:
                return mIsSender ? "正在发送" : "正在接收";
            case TRANSMIT_FINISHED:
                return mIsSender ? "发送结束" : "接收完成";
            case TRANSMIT_FAILED:
                return mIsSender ? "发送失败" : "接收失败";
        }
    }

    private class ViewHolder {
        ProgressBar mProgress;
        ImageView mStateView;
        Button mSendButton;
        ImageView mDeleteView;
        TextView mTitle;
        TextView mDetail;

        public ViewHolder(ProgressBar progress, ImageView stateView, Button sendButton, ImageView deleteView, TextView title, TextView detail) {
            mProgress = progress;
            mStateView = stateView;
            mSendButton = sendButton;
            mDeleteView = deleteView;
            mTitle = title;
            mDetail = detail;
        }
    }

    public class TransmittableMediaItem {
        private static final int MAX_PROGRESS = 100;

        MediaItem mMediaItem;
        int mProgress = 0;
        TransmitState mState = TransmitState.TRANSMIT_IDLE;

        public TransmittableMediaItem(MediaItem mediaItem, int progress, TransmitState state) {
            mMediaItem = mediaItem;
            mProgress = progress;
            mState = state;
        }
    }

    public enum TransmitState{
        TRANSMIT_IDLE(0),
        TRANSMITTING(1),
        TRANSMIT_FINISHED(2),
        TRANSMIT_FAILED(3);

        int mValue;
        TransmitState(int i) {
            mValue = i;
        }

        public int value() {
            return mValue;
        }

        public TransmitState valueOf(int value) {
            if (value >= 0 && value <= 3) {
                return TransmitState.values()[value];
            }

            return TRANSMIT_IDLE;
        }
    }
}
