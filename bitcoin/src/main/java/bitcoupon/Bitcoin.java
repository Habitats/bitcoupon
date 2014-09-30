package bitcoupon;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;

import java.math.BigInteger;

public class Bitcoin {

  private static final char[]
      BASE58ALPHABET =
      "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
  private static final byte[]
      BASE58VALUES =
      {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
       -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -1, -1, -1, -1, -1,
       -1, 9, 10, 11, 12, 13, 14, 15, 16, -1, 17, 18, 19, 20, 21, -1,
       22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, -1, -1, -1, -1, -1,
       -1, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, -1, 44, 45, 46,
       47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, -1, -1, -1, -1, -1,
       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
       -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

  private static final ECDomainParameters EC_PARAMS;

  static {
    X9ECParameters params = SECNamedCurves.getByName("secp256k1");
    EC_PARAMS =
        new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH(), params.getSeed());
  }

  static BigInteger decodePrivateKey(String encodedPrivateKey) {
    byte[] data = decodeBase58(encodedPrivateKey);
    if (data != null && data.length == 33 && (data[0] & 0xff) == 0x80) {
      byte[] decodedPrivateKey = new byte[32];
      System.arraycopy(data, 1, decodedPrivateKey, 0, 32);
      return new BigInteger(1, decodedPrivateKey);
    }
    return null;
  }

  static byte[] generatePublicKey(BigInteger privateKey) {
    return EC_PARAMS.getG().multiply(privateKey).getEncoded();
  }

  static String publicKeyToAddress(byte[] publicKey) {
    byte[] hashedPublicKey = hash160(publicKey);
    byte[] data = new byte[hashedPublicKey.length + 1];
    System.arraycopy(hashedPublicKey, 0, data, 1, hashedPublicKey.length);
    return encodeBase58(data);
  }

  static byte[] hash256(byte[] bytes) {
    SHA256Digest firstDigest = new SHA256Digest();
    firstDigest.update(bytes, 0, bytes.length);
    byte[] firstHash = new byte[32];
    firstDigest.doFinal(firstHash, 0);
    SHA256Digest secondDigest = new SHA256Digest();
    secondDigest.update(firstHash, 0, firstHash.length);
    byte[] secondHash = new byte[32];
    secondDigest.doFinal(secondHash, 0);
    return secondHash;
  }

  static byte[] hash160(byte[] bytes) {
    SHA256Digest firstDigest = new SHA256Digest();
    firstDigest.update(bytes, 0, bytes.length);
    byte[] firstHash = new byte[32];
    firstDigest.doFinal(firstHash, 0);
    RIPEMD160Digest secondDigest = new RIPEMD160Digest();
    secondDigest.update(firstHash, 0, firstHash.length);
    byte[] secondHash = new byte[20];
    secondDigest.doFinal(secondHash, 0);
    return secondHash;
  }

  static String encodeBase58(byte[] data) {
    byte[] checksum = hash256(data);
    byte[] bytes = new byte[data.length + 4];
    System.arraycopy(data, 0, bytes, 0, data.length);
    System.arraycopy(checksum, 0, bytes, data.length, 4);
    BigInteger value = new BigInteger(1, bytes);
    BigInteger base = BigInteger.valueOf(58);
    BigInteger zero = BigInteger.valueOf(0);
    String string = "";
    while (!value.equals(zero)) {
      BigInteger[] divideAndRemainder = value.divideAndRemainder(base);
      value = divideAndRemainder[0];
      BigInteger remainder = divideAndRemainder[1];
      string = BASE58ALPHABET[remainder.intValue()] + string;
    }
    for (int i = 0; i < bytes.length && bytes[i] == 0; i++) {
      string = BASE58ALPHABET[0] + string;
    }
    return string;
  }

  static byte[] decodeBase58(String string) {
    BigInteger value = BigInteger.valueOf(0);
    BigInteger base = BigInteger.valueOf(58);
    for (int i = 0; i < string.length(); i++) {
      value = value.multiply(base);
      byte digit = BASE58VALUES[string.charAt(i)];
      value = value.add(BigInteger.valueOf(digit));
    }
    byte[] bValue = value.toByteArray();
    int zeros = 0;
    for (int i = 0; i < string.length() && string.charAt(i) == '1'; i++) {
      zeros++;
    }
    byte[] bytes;
    if (bValue[0] == 0) {
      bytes = new byte[bValue.length - 1 + zeros];
      System.arraycopy(bValue, 1, bytes, zeros, bValue.length - 1);
    } else {
      bytes = new byte[bValue.length + zeros];
      System.arraycopy(bValue, 0, bytes, zeros, bValue.length);
    }
    byte[] data = new byte[bytes.length - 4];
    System.arraycopy(bytes, 0, data, 0, data.length);
    byte[] checksum = hash256(data);
    for (int i = 0; i < 4; i++) {
      if (checksum[i] != bytes[data.length + i]) {
        return null;
      }
    }
    return data;
  }

  static byte[] intToByteArray(int i) {
    return new byte[]{
        (byte) ((i >> 24) & 0xff),
        (byte) ((i >> 16) & 0xff),
        (byte) ((i >> 8) & 0xff),
        (byte) ((i >> 0) & 0xff)
    };
  }

  static byte[] longToByteArray(long l) {
    return new byte[]{
        (byte) ((l >> 56) & 0xff),
        (byte) ((l >> 48) & 0xff),
        (byte) ((l >> 40) & 0xff),
        (byte) ((l >> 32) & 0xff),
        (byte) ((l >> 24) & 0xff),
        (byte) ((l >> 16) & 0xff),
        (byte) ((l >> 8) & 0xff),
        (byte) ((l >> 0) & 0xff)
    };
  }

}