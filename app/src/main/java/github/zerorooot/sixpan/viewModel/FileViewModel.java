package github.zerorooot.sixpan.viewModel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import github.zerorooot.sixpan.bean.ApiUrl;
import github.zerorooot.sixpan.bean.FileBean;
import github.zerorooot.sixpan.bean.OffLineBean;
import github.zerorooot.sixpan.bean.OffLineParse;
import github.zerorooot.sixpan.bean.OffLineQuotaBean;
import github.zerorooot.sixpan.bean.PositionBean;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Setter
@Getter
public class FileViewModel extends AndroidViewModel {
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private MutableLiveData<List<FileBean>> liveData = new MutableLiveData<>();
    private MutableLiveData<List<OffLineBean>> offLineLiveData = new MutableLiveData<>();
    private final ConcurrentHashMap<String, PositionBean> positionCache = new ConcurrentHashMap<>();

    private String token;
    private ConcurrentHashMap<String, List<FileBean>> fileListCache = new ConcurrentHashMap<>();
    private String path = "/";
    private MutableLiveData<String> pathLiveDate = new MutableLiveData<>();
    private BottomNavigationView bottomNavigationView;

    private int limitCount = 20;

    public FileViewModel(@NonNull Application application) {
        super(application);
    }

    public void setPath(String path) {
        this.path = path;
        pathLiveDate.postValue(path);
    }

    public void getAllFile(String parentPath) {
        String url = ApiUrl.LIST;
        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("parentPath", parentPath);
        bodyJson.addProperty("limit", -1);
        getFile(bodyJson, url);
    }

    /**
     * 获取文件
     *
     * @param parentPath 文件路径
     */
    public void getFile(String parentPath, int start, int limit) {
        String url = ApiUrl.LIST;
        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("parentPath", parentPath);
        bodyJson.addProperty("limit", limit);
        bodyJson.addProperty("start", start);
        getFile(bodyJson, url);
    }

    private void getFile(JsonObject bodyJson, String url) {
        network(bodyJson, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                errorLog("error-getFile", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplication(), "网路连接失败，请重试!!!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String body = Objects.requireNonNull(response.body()).string();
                JsonObject object = new Gson().fromJson(body, JsonObject.class);
                JsonArray dataList = object.getAsJsonArray("dataList");
                //{"success":false,"status":401,"reference":"UNAUTHORIZED","message":"Unauthorized"}
                //登录失败
                if (Objects.isNull(dataList)) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplication(), "token过期，请重新登录", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                if (dataList.size() == 0) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplication(), "加载完毕~", Toast.LENGTH_SHORT).show();
                    });
                }
                ArrayList<FileBean> fileBeanLinkedList = new ArrayList<>();
                for (int i = 0; i < dataList.size(); i++) {
                    FileBean fileBean = setTimeAndPathAndSize(dataList, i);
                    fileBeanLinkedList.add(fileBean);
                }

                List<FileBean> value = liveData.getValue();
                if (value == null) {
                    liveData.postValue(fileBeanLinkedList);
                } else {
                    value.addAll(fileBeanLinkedList);
                    liveData.postValue(value);
                }

            }
        });
    }

    /**
     * 更新缓存
     *
     * @param parentPath 文件路径
     */
    public void updateCache(String parentPath) {
        String url = ApiUrl.LIST;
        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("parentPath", parentPath);
        bodyJson.addProperty("limit", -1);

        network(bodyJson, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                errorLog("error-getFile", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplication(), "网路连接失败，请重试!!!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String body = Objects.requireNonNull(response.body()).string();
                JsonObject object = new Gson().fromJson(body, JsonObject.class);
                JsonArray dataList = object.getAsJsonArray("dataList");
                //{"success":false,"status":401,"reference":"UNAUTHORIZED","message":"Unauthorized"}
                //登录失败
                if (Objects.isNull(dataList)) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplication(), "token过期，请重新登录", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                ArrayList<FileBean> fileBeanLinkedList = new ArrayList<>();
                for (int i = 0; i < dataList.size(); i++) {
                    FileBean fileBean = setTimeAndPathAndSize(dataList, i);

                    fileBeanLinkedList.add(fileBean);
                }

                fileListCache.put(parentPath, fileBeanLinkedList);
            }
        });

    }

    /**
     * 重命名文件
     *
     * @param fileBean 要重命名的bean
     * @param newName  新文件名
     */
    public void rename(FileBean fileBean, String newName) {
        String url = ApiUrl.RENAME;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("identity", fileBean.getIdentity());
        jsonObject.addProperty("name", newName);
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                errorLog("rename-file", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplication(), "重命名 " + fileBean.getName() + " -> " + newName + " 成功", Toast.LENGTH_SHORT).show();
                });
                int size = limitCount;
                if (liveData.getValue() != null) {
                    size = liveData.getValue().size();
                }
                liveData.postValue(null);
                getFile(fileBean.getParentPath(), 0, size);
            }
        });
    }

    /**
     * 创建新文件夹
     *
     * @param path       新文件夹的目录
     * @param folderName 新文件夹名
     */
    public MutableLiveData<Boolean> createFolder(String path, String folderName) {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();

        String url = ApiUrl.CREATE_FOLDER;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("path", path);
        jsonObject.addProperty("name", folderName);
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                errorLog("createFolder", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplication(), "新建 " + folderName + " 成功", Toast.LENGTH_SHORT).show();
                });
                liveData.postValue(true);
                //因为新建了文件夹，所以需要多加一个
//                getAllFile(path, 0, liveData.getValue() == null ? limitCount : liveData.getValue().size() + 1);
            }
        });
        return liveData;
    }


    public MutableLiveData<List<FileBean>> search(String path, String fileName) {
        MutableLiveData<List<FileBean>> mutableLiveData = new MutableLiveData<>();
        String url = ApiUrl.LIST;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("limit", -1);
        jsonObject.addProperty("name", fileName);
        jsonObject.addProperty("parentIdentity", toMd5(path));
        jsonObject.addProperty("search", true);

        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                errorLog("search ", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JsonArray returnJson = new Gson().fromJson(Objects.requireNonNull(response.body()).string(), JsonObject.class).getAsJsonArray("dataList");
                ArrayList<FileBean> fileBeanArrayList = new ArrayList<>();
                for (int i = 0; i < returnJson.size(); i++) {
                    FileBean fileBean = setTimeAndPathAndSize(returnJson, i);
                    if (!fileBean.isDeleted()) {
                        fileBeanArrayList.add(fileBean);
                    }

                }

                mutableLiveData.postValue(fileBeanArrayList);
            }
        });
        return mutableLiveData;
    }

    /**
     * 获取单个文件下载url
     *
     * @param identity 文件识别号
     */
    public MutableLiveData<String> downloadSingle(String identity) {
        MutableLiveData<String> liveData = new MutableLiveData<>();
        String url = ApiUrl.DOWNLOAD;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("identity", identity);
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                errorLog("downloadSingle", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JsonObject returnJson = new Gson().fromJson(Objects.requireNonNull(response.body()).string(), JsonObject.class);
                if (returnJson.get("downloadAddress") != null) {
                    liveData.postValue(returnJson.get("downloadAddress").getAsString());
                    return;
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplication(), "获取下载地址失败，请刷新后再试试", Toast.LENGTH_SHORT).show();
                });
            }
        });
        return liveData;
    }

    /**
     * 打包下载
     *
     * @param fileBeanArrayList list
     */
    public MutableLiveData<String> downloadZip(ArrayList<FileBean> fileBeanArrayList) {
        MutableLiveData<String> liveData = new MutableLiveData<>();
        String url = ApiUrl.PACKUP_DOWNLOAD;
        JsonArray jsonArray = new JsonArray();
        for (FileBean fileBean : fileBeanArrayList) {
            jsonArray.add(fileBean.getIdentity());
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("sourceIdentity", jsonArray);
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                errorLog("downloadZip", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JsonObject returnJson = new Gson().fromJson(Objects.requireNonNull(response.body()).string(), JsonObject.class);
                //{"success":false,"status":422,"reference":"TOTAL_SIZE_LIMIT","message":"文件总大小超过限制"}
                if (Objects.nonNull(returnJson.get("success"))) {
                    //Looper.getMainLooper()
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplication().getApplicationContext(), returnJson.toString(), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                liveData.postValue(returnJson.get("downloadAddress").getAsString());
            }
        });
        return liveData;
    }


    /**
     * 移动文件
     *
     * @param fileBeanArrayList 要移动的list
     * @param newPath           移动的目录
     */
    public MutableLiveData<Boolean> move(ArrayList<FileBean> fileBeanArrayList, String newPath) {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();
        String url = ApiUrl.MOVE;
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        fileBeanArrayList.forEach(s -> jsonArray.add(s.getIdentity()));
        jsonObject.add("sourceIdentity", jsonArray);
        jsonObject.addProperty("path", newPath);
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                errorLog("move-file", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JsonObject jsonBody = new Gson().fromJson(Objects.requireNonNull(response.body()).string(), JsonObject.class);
                //{"success":false,"status":404,"reference":"FILE_NOT_FOUND","message":"文件未找到"}
                //{"success":false,"status":422,"reference":"FILE_ALREADY_EXISTS","message":"File already exists"}

                //{"successCount":1}
                if (Objects.nonNull(jsonBody.get("success"))) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplication().getApplicationContext(), "移动失败，" + jsonBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplication().getApplicationContext(), "移动成功", Toast.LENGTH_SHORT).show();
                    });
                    liveData.postValue(true);


                    Set<String> set = fileBeanArrayList.stream()
                            .map(FileBean::getParentPath).collect(Collectors.toSet())
                            //删除包含新路径的元素，因为外部会更新
                            .stream().filter(s -> !s.equals(newPath)).collect(Collectors.toSet());
                    set.forEach(s -> {
                        updateCache(s);
                    });
//                    updateCache(fileBeanArrayList.get(0).getParentPath());
                }

            }
        });
        return liveData;
    }


    public void delete(FileBean fileBean) {
        List<FileBean> fileBeanList = new ArrayList<>();
        fileBeanList.add(fileBean);
        delete(fileBeanList);
    }

    /**
     * delete file bean list
     *
     * @param fileBeanList list
     */
    public void delete(List<FileBean> fileBeanList) {
        String url = ApiUrl.DELETE;
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        fileBeanList.forEach(s -> jsonArray.add(s.getIdentity()));
        jsonObject.add("sourceIdentity", jsonArray);


        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                errorLog("delete-file", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplication().getApplicationContext(), "删除成功", Toast.LENGTH_SHORT).show();
                });

//                getAllFile(fileBeanList.get(0).getParentPath());
                //updateCache(fileBeanList.get(0).getParentPath());
            }
        });
    }


    public MutableLiveData<byte[]> downloadFile(String url) {
        MutableLiveData<byte[]> liveData = new MutableLiveData<>();
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new Handler(Looper.myLooper()).post(() -> {
                    Toast.makeText(getApplication().getApplicationContext(), "下载失败~", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                liveData.postValue(Objects.requireNonNull(response.body()).bytes());
            }
        });
        return liveData;
    }

    public void logout() {
        new Thread(() -> {
            try {
                network(new JsonObject(), ApiUrl.LOGOUT).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
//---------------------------------------------------------离线相关----------------------------------------------------

    /**
     * 获取离线列表
     */
    public void getOffLineList() {
        String url = ApiUrl.OFFLINE_LIST;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("limit", -1);
        jsonObject.addProperty("start", 0);
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JsonObject returnJson = new Gson().fromJson(response.body().string(), JsonObject.class);
                JsonArray jsonArray = returnJson.getAsJsonArray("dataList");
                List<OffLineBean> offLineBeanList = new ArrayList<>();

                jsonArray.forEach(json -> {
                    OffLineBean offLineBean = new Gson().fromJson(json.getAsJsonObject(), OffLineBean.class);
                    offLineBean.setTime(DateUtil.date(offLineBean.getCreateTime()).toString());
                    offLineBean.setSizeString(DataSizeUtil.format(offLineBean.getSize()));
                    if ("text/directory".equals(offLineBean.getFileMime())) {
                        offLineBean.setDirectory(true);
                    }
                    offLineBeanList.add(offLineBean);
                });

                offLineLiveData.postValue(offLineBeanList);
            }
        });
    }


    /**
     * 获取离线配额
     *
     * @return live data
     */
    public MutableLiveData<OffLineQuotaBean> quota() {
        MutableLiveData<OffLineQuotaBean> liveData = new MutableLiveData<>();
        String url = ApiUrl.QUOTA;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("time", System.currentTimeMillis());
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                OffLineQuotaBean quotaBean = new Gson().fromJson(response.body().string(), OffLineQuotaBean.class);
                liveData.postValue(quotaBean);
            }
        });
        return liveData;
    }

    public void deleteOfflineFile(OffLineBean offLineBean) {
        List<OffLineBean> lineBeans = new ArrayList<>();
        lineBeans.add(offLineBean);
        deleteOfflineFile(lineBeans);
    }

    public void deleteOfflineFile(List<OffLineBean> offLineBeanList) {
        String url = ApiUrl.OFFLINE_DELETE;
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        offLineBeanList.forEach(s -> {
            jsonArray.add(s.getTaskIdentity());
        });
        jsonObject.add("taskIdentity", jsonArray);
        jsonObject.addProperty("deleteFile", false);
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JsonObject result = new Gson().fromJson(response.body().string(), JsonObject.class);
                if (result.has("successCount")) {
                    getOffLineList();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplication().getApplicationContext(), "删除离线任务成功~", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    public void deleteCompleteOffline() {
        String url = ApiUrl.DELETE_COMPLETE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", 1000);
        jsonObject.addProperty("deleteFile", false);
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                getOffLineList();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplication().getApplicationContext(), "删除完成任务成功~", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    public MutableLiveData<OffLineParse> offLineParse(String link, String password) {
        String url = ApiUrl.PARSE;
        MutableLiveData<OffLineParse> liveData = new MutableLiveData<>();
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("textLink", link);
        if (Objects.nonNull(password)) {
            jsonObject.addProperty("password", password);
        }

        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String body = Objects.requireNonNull(response.body()).string();
                JsonObject result = new Gson().fromJson(body, JsonObject.class);
                OffLineParse parse;
                //{"success":false,"status":422,"reference":"UNSUPPORT_URL","message":"不支持的链接"}
                if (result.has("hash")) {
                    String hash = result.get("hash").getAsString();
                    JsonObject info = result.getAsJsonObject("info");
                    parse = new Gson().fromJson(info, OffLineParse.class);
                    parse.setSizeString(DataSizeUtil.format(parse.getSize()));
                    parse.setHash(hash);
                    parse.setReady(true);
                } else {
                    parse = new OffLineParse();
                    parse.setTextLink(link);
                    parse.setName(result.get("message").getAsString());
                    parse.setReady(false);
                }
                liveData.postValue(parse);
            }
        });
        return liveData;
    }

    public MutableLiveData<Integer> offLineDownload(String path, List<String> hashList) {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        JsonArray jsonArray = new JsonArray();
        hashList.forEach(s -> {
            JsonObject object = new JsonObject();
            object.addProperty("hash", s);
            jsonArray.add(object);
        });

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("savePath", path);
        jsonObject.add("task", jsonArray);

        String url = ApiUrl.ADD_OFFLINE;
        network(jsonObject, url).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String body = response.body().string();
                JsonObject jsonObject = new Gson().fromJson(body, JsonObject.class);
                if (jsonObject.has("successCount")) {
                    liveData.postValue(jsonObject.get("successCount").getAsInt());
                }
            }
        });
        return liveData;
    }

    private Call network(JsonObject jsonObject, String url) {
        MediaType mediaType = MediaType.parse("application/json");
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonObject.toString().getBytes(), mediaType))
                .addHeader("authorization", token)
                .addHeader("accept-language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .build();

        return okHttpClient.newCall(request);
    }

    /**
     * 设置bean中的文件大小和路径
     *
     * @param returnJson json字符串
     * @param i          位置
     * @return bean
     */
    private FileBean setTimeAndPathAndSize(JsonArray returnJson, int i) {
        JsonObject object = returnJson.get(i).getAsJsonObject();
        FileBean fileBean = new Gson().fromJson(object.toString(), FileBean.class);
        fileBean.setDateTime(DateUtil.date(fileBean.getAtime()).toString());
        String path = fileBean.getPath();
        if (path.lastIndexOf("/") == 0) {
            fileBean.setParentPath("/");
        } else {
            fileBean.setParentPath(path.substring(0, path.lastIndexOf("/")));
        }
        long size = fileBean.getSize();
        fileBean.setSizeString(DataSizeUtil.format(size));
        return fileBean;
    }

    /**
     * out put error log
     *
     * @param tag tag
     * @param e   exception
     */
    private void errorLog(String tag, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString();
        Log.e(tag, sStackTrace);
    }

    private String toMd5(String str) {
        StringBuilder hexString = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] hash = md.digest();
            for (byte b : hash) {
                if ((0xff & b) < 0x10) {
                    hexString.append("0").append(Integer.toHexString((0xFF & b)));
                } else {
                    hexString.append(Integer.toHexString(0xFF & b));
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }

    public void scroll(String path, RecyclerView recycleView) {
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recycleView.getLayoutManager();
        PositionBean position = positionCache.get(path);
        if (position == null) {
            return;
        }

        recycleView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                assert linearLayoutManager != null;
                linearLayoutManager.scrollToPositionWithOffset(position.getLastPosition(), position.getLastOffset());
                recycleView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }

    public void setPosition(String path, LinearLayoutManager linearLayoutManager) {
        //获取可视的第一个view
        if (linearLayoutManager == null) {
            return;
        }

        View topView = linearLayoutManager.getChildAt(0);
        //获取与该view的顶部的偏移量
        if (topView != null) {
            int lastOffset = topView.getTop();
            int lastPosition = linearLayoutManager.getPosition(topView);
            PositionBean positionBean = new PositionBean();
            positionBean.setLastOffset(lastOffset);
            positionBean.setLastPosition(lastPosition);

            positionCache.put(path, positionBean);
        }

    }
}
