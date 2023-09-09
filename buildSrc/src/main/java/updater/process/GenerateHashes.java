package updater.process;

import arc.files.Fi;
import updater.Vars;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GenerateHashes {
    public static void process(Fi folder) throws NoSuchAlgorithmException {

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        folder.walk(it -> {
            String extension = it.extension();
            if(extension.matches("md5|sha1"))return;
            byte[] data = it.readBytes();
            it.sibling(it.name()+".md5").writeString(hashString(data,md5));
            it.sibling(it.name()+".sha1").writeString(hashString(data,sha1));
        });
    }
    private static String hashString(byte[] message, MessageDigest algorithm) {

            byte[] hashedBytes = algorithm.digest(message);
            return convertByteArrayToHexString(hashedBytes);
    }private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }
}
