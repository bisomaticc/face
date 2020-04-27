package com.megvii.facepp.sdk;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FeatureRestoreHelper {
    public static final byte[] sMagic = {1, 2, 3, 4, 5, 6, 7, 8};
    private UnlockEncryptor mEncryptor;

    public void setUnlockEncryptor(UnlockEncryptor unlockEncryptor) {
        this.mEncryptor = unlockEncryptor;
    }

    public void saveRestoreImage(byte[] bArr, String str, int i) {
        Log.i("FeatureRestoreHelper", "saveRestoreImage: length: " + bArr.length + " id " + i);
        writeFile(getRestoreFile(str, i).getAbsolutePath(), bArr);
    }

    public void deleteRestoreImage(String str, int i) {
        Log.i("FeatureRestoreHelper", "deleteRestoreImage: id " + i);
        getRestoreFile(str, i).delete();
    }

    public int restoreAllFeature(String str) {
        int i;
        File[] listFiles = new File(str).listFiles();
        if (listFiles.length == 0) {
            return 24;
        }
        int i2 = 0;
        for (File file : listFiles) {
            String name = file.getName();
            if (name.startsWith("restore_") && name.length() > 8) {
                Log.i("FeatureRestoreHelper", "restoreAllFeature: " + name);
                try {
                    i = Integer.parseInt(name.substring(8));
                } catch (NumberFormatException unused) {
                    i = -1;
                }
                if (i != -1) {
                    byte[] readFile = readFile(file.getAbsolutePath());
                    Log.i("FeatureRestoreHelper", "restoreAllFeature: update old feature " + i);
                    if (restoreFeatureAtPosition(i, readFile) == 0) {
                        i2++;
                    }
                }
            }
        }
        if (i2 == 0) {
            return 24;
        }
        return 0;
    }

    private int restoreFeatureAtPosition(int i, byte[] bArr) {
        return Lite.getInstance().updateFeature(bArr, 144, 144, 90, true, new byte[10000], new byte[40000], i);
    }

    private File getRestoreFile(String str, int i) {
        return new File(str, "restore_" + i);
    }

    private void writeFile(String str, byte[] bArr) {
        File file = new File(str);
        if (file.exists()) {
            file.delete();
        }
        UnlockEncryptor unlockEncryptor = this.mEncryptor;
        int i = 0;
        if (unlockEncryptor != null) {
            byte[] encrypt = unlockEncryptor.encrypt(bArr);
            int length = encrypt.length;
            byte[] bArr2 = sMagic;
            bArr = new byte[(length + bArr2.length)];
            System.arraycopy(bArr2, 0, bArr, 0, bArr2.length);
            System.arraycopy(encrypt, 0, bArr, sMagic.length, encrypt.length);
        }
        int length2 = bArr.length;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(str);
            while (length2 > i) {
                int i2 = length2 - i;
                if (i2 > 8192) {
                    i2 = 8192;
                }
                fileOutputStream.write(bArr, i, i2);
                i += i2;
            }
        } catch (IOException e) {
            Log.e("FeatureRestoreHelper", "writeFile failed", e);
        }
    }

    private byte[] readFile(String str) {
        File file = new File(str);
        if (!file.exists()) {
            return null;
        }
        int length = (int) file.length();
        byte[] bArr = new byte[length];
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            int i = 0;
            while (length > i) {
                int i2 = length - i;
                if (i2 > 8192) {
                    i2 = 8192;
                }
                i += fileInputStream.read(bArr, i, i2);
            }
            if (!(this.mEncryptor == null || this.mEncryptor == null)) {
                if (startWithMagic(bArr)) {
                    byte[] bArr2 = new byte[(bArr.length - sMagic.length)];
                    System.arraycopy(bArr, sMagic.length, bArr2, 0, bArr2.length);
                    return this.mEncryptor.decrypt(bArr2);
                }
            }
            return bArr;
        } catch (IOException e) {
            Log.e("FeatureRestoreHelper", "readFile failed", e);
            return bArr;
        }
    }

    private boolean startWithMagic(byte[] bArr) {
        if (bArr.length < sMagic.length) {
            return false;
        }
        int i = 0;
        while (true) {
            byte[] bArr2 = sMagic;
            if (i >= bArr2.length) {
                return true;
            }
            if (bArr[i] != bArr2[i]) {
                return false;
            }
            i++;
        }
    }
}
