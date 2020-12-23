package com.applozic.mobicomkit.uiwidgets.conversation.richmessaging;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.FullScreenImageActivity;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.MobiComKitActivityInterface;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.callbacks.ALRichMessageListener;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.helpers.KmFormStateHelper;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.models.ALBookingDetailsModel;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.models.ALGuestCountModel;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.models.ALRichMessageModel;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.models.AlHotelBookingModel;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.models.KmFormStateModel;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.models.v2.KmFormPayloadModel;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.models.v2.KmRMActionModel;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.webview.AlWebViewActivity;
import com.applozic.mobicomkit.uiwidgets.kommunicate.adapters.KmAutoSuggestionAdapter;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.kommunicate.KmSettings;
import io.kommunicate.Kommunicate;
import io.kommunicate.async.KmPostDataAsyncTask;
import io.kommunicate.callbacks.KmCallback;
import io.kommunicate.models.KmAutoSuggestionModel;

public class RichMessageActionProcessor implements ALRichMessageListener {

    private final ALRichMessageListener richMessageListener;
    public static final String NOTIFY_ITEM_CHANGE = "notifyItemChange";
    private static final String TAG = "AlRichMessageAction";

    public RichMessageActionProcessor(ALRichMessageListener richMessageListener) {
        this.richMessageListener = richMessageListener;
    }

    public ALRichMessageListener getRichMessageListener() {
        return this;
    }

    @Override
    public void onAction(Context context, String action, Message message, Object object, Map<String, Object> replyMetadata) {
        switch (action) {
            case AlRichMessage.SEND_GUEST_LIST:
                List<ALGuestCountModel> guestCountModels = (List<ALGuestCountModel>) object;
                sendGuestListMessage(guestCountModels, getStringMap(replyMetadata));
                break;

            case AlRichMessage.SEND_HOTEL_RATING:
                sendMessage((String) object, getStringMap(replyMetadata));
                break;

            case AlRichMessage.SEND_HOTEL_DETAILS:
                sendHotelDetailMessage((AlHotelBookingModel) object, getStringMap(replyMetadata));
                break;

            case AlRichMessage.SEND_ROOM_DETAILS_MESSAGE:
                sendRoomDetailsMessage((AlHotelBookingModel) object, getStringMap(replyMetadata));
                break;

            case AlRichMessage.SEND_BOOKING_DETAILS:
                sendBookingDetailsMessage((ALBookingDetailsModel) object, getStringMap(replyMetadata));
                break;

            case AlRichMessage.MAKE_PAYMENT:
            case AlRichMessage.SUBMIT_BUTTON:
                if (object instanceof KmRMActionModel.SubmitButton) {
                    handleKmSubmitButton(context, message, (KmRMActionModel.SubmitButton) object);
                } else {
                    handleSubmitButton(context, object);
                }
                break;

            case AlRichMessage.QUICK_REPLY_OLD:
            case AlRichMessage.QUICK_REPLY:
                if (object instanceof String) {
                    sendMessage((String) object, getStringMap(replyMetadata));
                } else {
                    handleQuickReplies(object, replyMetadata);
                }
                break;

            case AlRichMessage.TEMPLATE_ID + 9:
                loadImageOnFullScreen(context, action, (ALRichMessageModel.ALPayloadModel) object);
                break;

            case AlRichMessage.WEB_LINK:
                handleWebLinks(object);
                break;
            case KmAutoSuggestionAdapter.KM_AUTO_SUGGESTION_ACTION:
                try {
                    KmAutoSuggestionModel autoSuggestionModel = (KmAutoSuggestionModel) object;
                    if (richMessageListener != null) {
                        richMessageListener.onAction(context, KmAutoSuggestionAdapter.KM_AUTO_SUGGESTION_ACTION, null, autoSuggestionModel.getContent(), null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public void handleWebLinks(Object object) {
        ALRichMessageModel.AlAction alAction = null;

        if (object instanceof ALRichMessageModel.AlButtonModel) {
            alAction = ((ALRichMessageModel.AlButtonModel) object).getAction();
        } else if (object instanceof ALRichMessageModel.AlElementModel) {
            alAction = ((ALRichMessageModel.AlElementModel) object).getAction();
        } else if (object instanceof ALRichMessageModel.AlAction) {
            alAction = (ALRichMessageModel.AlAction) object;
        } else if (object instanceof ALRichMessageModel.ALPayloadModel) {
            alAction = ((ALRichMessageModel.ALPayloadModel) object).getAction();
        }

        if (alAction != null) {
            if (!TextUtils.isEmpty(alAction.getUrl())) {
                openWebLink(alAction.getUrl(), alAction.isDeepLink());
            } else if (alAction.getPayload() != null && !TextUtils.isEmpty(alAction.getPayload().getUrl())) {
                openWebLink(alAction.getPayload().getUrl(), alAction.getPayload().isDeepLink());
            }
        }

        if (object instanceof ALRichMessageModel.ALPayloadModel) {
            ALRichMessageModel.ALPayloadModel payloadModel = (ALRichMessageModel.ALPayloadModel) object;
            if (!TextUtils.isEmpty(payloadModel.getUrl())) {
                openWebLink(payloadModel.getUrl(), payloadModel.isDeepLink());
            }
        }
    }

    private void updateLanguage(String languageCode) {
        if (!TextUtils.isEmpty(languageCode)) {
            KmSettings.updateUserLanguage(ApplozicService.getAppContext(), languageCode);
        }
    }

    public void handleQuickReplies(Object object, Map<String, Object> replyMetadata) {
        String message = null;

        if (object instanceof ALRichMessageModel.ALPayloadModel) {
            ALRichMessageModel.ALPayloadModel payloadModel = (ALRichMessageModel.ALPayloadModel) object;
            updateLanguage(payloadModel.getUpdateLanguage());
            if (payloadModel.getAction() != null && !TextUtils.isEmpty(payloadModel.getAction().getMessage())) {
                handleQuickReplies(payloadModel.getAction(), payloadModel.getReplyMetadata());
            } else {
                message = !TextUtils.isEmpty(payloadModel.getMessage()) ? payloadModel.getMessage() : payloadModel.getName();
            }
        } else if (object instanceof ALRichMessageModel.AlButtonModel) {
            ALRichMessageModel.AlButtonModel buttonModel = (ALRichMessageModel.AlButtonModel) object;
            if (isValidAction(buttonModel.getAction())) {
                handleQuickReplies(buttonModel.getAction(), replyMetadata);
            } else {
                message = buttonModel.getName();
            }
        } else if (object instanceof ALRichMessageModel.AlAction) {
            ALRichMessageModel.AlAction action = (ALRichMessageModel.AlAction) object;
            updateLanguage(action.getUpdateLanguage());
            if (action.getPayload() != null) {
                updateLanguage(action.getPayload().getUpdateLanguage());
                if (!TextUtils.isEmpty(action.getPayload().getMessage())) {
                    message = action.getPayload().getMessage();
                } else if (!TextUtils.isEmpty(action.getPayload().getTitle())) {
                    message = action.getPayload().getTitle();
                }
            } else {
                message = !TextUtils.isEmpty(action.getMessage()) ? action.getMessage() : !TextUtils.isEmpty(action.getText()) ? action.getText() : !TextUtils.isEmpty(action.getTitle()) ? action.getTitle() : action.getName();
            }
        } else if (object instanceof ALRichMessageModel.AlElementModel) {
            ALRichMessageModel.AlElementModel elementModel = (ALRichMessageModel.AlElementModel) object;
            if (replyMetadata == null) {
                replyMetadata = new HashMap<>();
            }
            if (elementModel.getArticleId() != null) {
                replyMetadata.put(AlRichMessage.KM_FAQ_ID, elementModel.getArticleId());
            }
            if (!TextUtils.isEmpty(elementModel.getSource())) {
                replyMetadata.put(AlRichMessage.KM_SOURCE, elementModel.getSource());
            }

            if (isValidAction(elementModel.getAction())) {
                handleQuickReplies(elementModel.getAction(), replyMetadata);
            } else {
                message = elementModel.getTitle();
            }
        }

        if (!TextUtils.isEmpty(message)) {
            sendMessage(message, getStringMap(replyMetadata));
        }
    }

    public boolean isValidAction(ALRichMessageModel.AlAction action) {
        return action != null && (action.getPayload() != null || !TextUtils.isEmpty(action.getText()));
    }

    public void handleSubmitButton(Context context, Object object) {
        if (object instanceof KmRMActionModel.SubmitButton) {
            KmRMActionModel.SubmitButton submitButton = (KmRMActionModel.SubmitButton) object;
            // Need to implement
        } else if (object instanceof ALRichMessageModel.AlButtonModel) {
            ALRichMessageModel.AlButtonModel buttonModel = (ALRichMessageModel.AlButtonModel) object;
            if (buttonModel.getAction() != null && buttonModel.getAction().getPayload() != null) {
                openWebLink(GsonUtils.getJsonFromObject(buttonModel.getAction().getPayload().getFormData(), ALRichMessageModel.AlFormDataModel.class)
                        , buttonModel.getAction().getPayload().getFormAction());
            }
        } else if (object instanceof ALRichMessageModel) {
            ALRichMessageModel model = (ALRichMessageModel) object;
            openWebLink(model.getFormData(), model.getFormAction());
        } else if (object instanceof ALRichMessageModel.ALPayloadModel) {
            makeFormRequest(context, (ALRichMessageModel.ALPayloadModel) object);
        }
    }

    private boolean isInvalidData(Map<String, Object> dataMap, KmRMActionModel.SubmitButton submitButton) {
        return (dataMap == null || dataMap.isEmpty()) && (submitButton.getFormData() == null || submitButton.getFormData().isEmpty());
    }

    public void handleKmSubmitButton(final Context context, final Message message, final KmRMActionModel.SubmitButton submitButtonModel) {
        KmFormStateModel formStateModel = null;
        if (message != null) {
            formStateModel = KmFormStateHelper.getFormState(message.getKeyString());
        }
        final Map<String, Object> dataMap = KmFormStateHelper.getKmFormMap(message, formStateModel);

        if (isInvalidData(dataMap, submitButtonModel)) {
            KmToast.error(context, Utils.getString(context, R.string.km_invalid_form_data_error), Toast.LENGTH_SHORT).show();
            return;
        }

        Utils.printLog(context, TAG, "Submitting data : " + GsonUtils.getJsonFromObject(formStateModel != null ? dataMap : submitButtonModel.getFormData(), Map.class));

        if (KmRMActionModel.SubmitButton.KM_POST_DATA_TO_BOT_PLATFORM.equals(submitButtonModel.getRequestType())) {
            sendMessage(submitButtonModel.getMessage(), getStringMap(submitButtonModel.getReplyMetadata()), dataMap, submitButtonModel.getFormData());
            if (richMessageListener != null) {
                richMessageListener.onAction(context, NOTIFY_ITEM_CHANGE, message, dataMap, submitButtonModel.getReplyMetadata());
            }
        } else {
            if (!TextUtils.isEmpty(submitButtonModel.getMessage())) {
                sendMessage(submitButtonModel.getMessage(), getStringMap(submitButtonModel.getReplyMetadata()));
            }
            new KmPostDataAsyncTask(context,
                    submitButtonModel.getFormAction(),
                    null,
                    AlWebViewActivity.REQUEST_TYPE_JSON.equals(submitButtonModel.getRequestType()) ? "application/json" : AlWebViewActivity.DEFAULT_REQUEST_TYPE,
                    GsonUtils.getJsonFromObject(formStateModel != null ? dataMap : submitButtonModel.getFormData(), Map.class),
                    new KmCallback() {
                        @Override
                        public void onSuccess(Object messageString) {
                            Utils.printLog(context, TAG, "Submit post success : " + messageString);
                            if (richMessageListener != null) {
                                richMessageListener.onAction(context, NOTIFY_ITEM_CHANGE, message, dataMap, submitButtonModel.getReplyMetadata());
                            }
                        }

                        @Override
                        public void onFailure(Object error) {
                            Utils.printLog(context, TAG, "Submit post error : " + error);
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void sendMessage(String message, Map<String, String> replyMetadata, Map<String, Object> formSelectedData, Map<String, String> formData) {
        Map<String, String> metadata = new HashMap<>();
        if (replyMetadata != null) {
            metadata.putAll(replyMetadata);
        }
        if (formSelectedData != null) {
            Map<String, String> formDataMap = new HashMap<>();
            formDataMap.put(KmFormPayloadModel.KM_FORM_DATA, GsonUtils.getJsonFromObject(getStringMap(formSelectedData), Map.class));
            metadata.put(Kommunicate.KM_CHAT_CONTEXT, GsonUtils.getJsonFromObject(formDataMap, Map.class));
        } else {
            metadata.putAll(formData);
        }
        sendMessage(message, metadata);
    }

    public Map<String, String> getStringMap(Map<String, Object> objectMap) {
        if (objectMap == null) {
            return null;
        }
        Map<String, String> newMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
            newMap.put(entry.getKey(), entry.getValue() instanceof String ? (String) entry.getValue() : GsonUtils.getJsonFromObject(entry.getValue(), Object.class));
        }
        return newMap;
    }

    public void sendMessage(String message, Map<String, String> replyMetadata) {
        sendMessage(message, replyMetadata, Message.ContentType.DEFAULT.getValue());
    }

    public void openWebLink(String url, boolean isDeepLink) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(AlRichMessage.WEB_LINK, true);
        bundle.putString(AlRichMessage.LINK_URL, url);
        bundle.putBoolean(AlRichMessage.IS_DEEP_LINK, isDeepLink);
        if (richMessageListener != null) {
            richMessageListener.onAction(null, AlRichMessage.OPEN_WEB_VIEW_ACTIVITY, null, bundle, null);
        }
    }

    public void makeFormRequest(final Context context, ALRichMessageModel.ALPayloadModel payloadModel) {
        if (payloadModel != null && payloadModel.getAction() != null) {
            if (!TextUtils.isEmpty(payloadModel.getAction().getMessage())) {
                sendMessage(payloadModel.getAction().getMessage(), getStringMap(payloadModel.getReplyMetadata()));
            } else if (!TextUtils.isEmpty(payloadModel.getAction().getName())) {
                sendMessage(payloadModel.getAction().getName(), getStringMap(payloadModel.getReplyMetadata()));
            }

            new KmPostDataAsyncTask(context,
                    payloadModel.getAction().getFormAction(),
                    null,
                    AlWebViewActivity.REQUEST_TYPE_JSON.equals(payloadModel.getRequestType()) ? "application/json" : AlWebViewActivity.DEFAULT_REQUEST_TYPE,
                    GsonUtils.getJsonFromObject(payloadModel.getFormData(), ALRichMessageModel.AlFormDataModel.class),
                    new KmCallback() {
                        @Override
                        public void onSuccess(Object message) {
                            Utils.printLog(context, TAG, "Submit post success : " + message);
                        }

                        @Override
                        public void onFailure(Object error) {
                            Utils.printLog(context, TAG, "Submit post error : " + error);
                        }
                    }).execute();
        }
    }

    public void openWebLink(String formData, String formAction) {
        Bundle bundle = new Bundle();
        if (!TextUtils.isEmpty(formData)) {
            bundle.putString(AlRichMessage.KM_FORM_DATA, formData);
        }
        if (!TextUtils.isEmpty(formAction)) {
            bundle.putString(AlRichMessage.KM_FORM_ACTION, formAction);
        }

        if (richMessageListener != null) {
            richMessageListener.onAction(null, AlRichMessage.OPEN_WEB_VIEW_ACTIVITY, null, bundle, null);
        }
    }

    public void sendGuestListMessage(List<ALGuestCountModel> guestList, Map<String, String> replyMetadata) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("guestTypeId", "ADULTS");
        metadata.put("isRoomGuestJSON", "true");
        metadata.put("roomGuestJson", GsonUtils.getJsonFromObject(guestList, List.class));

        StringBuilder message = new StringBuilder();
        int count = 0;

        for (ALGuestCountModel guestModel : guestList) {
            message.append("Room ");
            message.append(count + 1);
            message.append(" Guest ");
            message.append(guestModel.getNoOfAdults());
            message.append(" Children ");
            message.append(guestModel.getNoOfChild());
            message.append(", ");
        }

        if (replyMetadata != null) {
            metadata.putAll(replyMetadata);
        }

        sendMessage(message.toString(), metadata, Message.ContentType.DEFAULT.getValue());
    }

    public void sendHotelDetailMessage(AlHotelBookingModel hotel, Map<String, String> replyMetadata) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("hotelSelected", "true");
        metadata.put("resultIndex", String.valueOf(hotel.getResultIndex()));
        metadata.put("sessionId", hotel.getSessionId());
        metadata.put("skipBot", "true");

        String message = "Get room detail of " + hotel.getHotelName();

        if (replyMetadata != null) {
            metadata.putAll(replyMetadata);
        }

        sendMessage(message, metadata, Message.ContentType.DEFAULT.getValue());
    }

    public void sendRoomDetailsMessage(AlHotelBookingModel hotel, Map<String, String> replyMetadata) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("HotelResultIndex", String.valueOf(hotel.getHotelResultIndex()));
        metadata.put("NoOfRooms", String.valueOf(hotel.getNoOfRooms()));
        metadata.put("RoomIndex", String.valueOf(hotel.getRoomIndex()));
        metadata.put("blockHotelRoom", "true");
        metadata.put("sessionId", hotel.getSessionId());
        metadata.put("skipBot", "true");

        if (replyMetadata != null) {
            metadata.putAll(replyMetadata);
        }

        String message = "Book Hotel " + hotel.getHotelName() + ", Room " + hotel.getRoomTypeName();

        sendMessage(message, metadata, Message.ContentType.DEFAULT.getValue());
    }

    public void sendBookingDetailsMessage(ALBookingDetailsModel model, Map<String, String> replyMetadata) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("guestDetail", "true");
        metadata.put("personInfo", GsonUtils.getJsonFromObject(model.getPersonInfo(), ALBookingDetailsModel.ALBookingDetails.class));
        metadata.put("sessionId", model.getSessionId());
        metadata.put("skipBot", "true");

        if (replyMetadata != null) {
            metadata.putAll(replyMetadata);
        }

        sendMessage("Your details have been submitted", metadata, Message.ContentType.DEFAULT.getValue());
    }

    public void loadImageOnFullScreen(Context context, String action, ALRichMessageModel.ALPayloadModel payloadModel) {
        Intent intent = new Intent(context, FullScreenImageActivity.class);
        intent.putExtra(action, GsonUtils.getJsonFromObject(payloadModel, ALRichMessageModel.ALPayloadModel.class));
        ((MobiComKitActivityInterface) context).startActivityForResult(intent, MobiComKitActivityInterface.REQUEST_CODE_FULL_SCREEN_ACTION);
    }

    public void sendMessage(String message, Map<String, String> messageMetaData, Short contentType) {
        if (richMessageListener != null) {
            Message messageToSend = new Message();
            messageToSend.setMessage(message);
            messageToSend.setMetadata(messageMetaData);
            messageToSend.setContentType(contentType);
            richMessageListener.onAction(null, AlRichMessage.SEND_MESSAGE, messageToSend, null, null);
        }
    }
}
