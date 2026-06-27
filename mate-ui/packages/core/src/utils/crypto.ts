import CryptoJS from 'crypto-js'

/**
 * Password transport encryption — AES-CFB, matching the backend
 * {@code PasswordTransportDecryptor}.
 *
 * The key is a fixed 16-char string shared between frontend and backend.
 * In AES-CFB mode, both key and IV use the same 16-byte value.
 *
 * @param plainText  raw password
 * @param key        16-char AES key (default: built-in transport key)
 * @returns          Base64-encoded ciphertext
 */
const DEFAULT_TRANSPORT_KEY = 'mate9x!K#2qL8pZw'  // 16 chars = 128-bit AES

export function encryptPassword(plainText: string, key = DEFAULT_TRANSPORT_KEY): string {
  const keyBytes = CryptoJS.enc.Utf8.parse(key)
  const ivBytes  = CryptoJS.enc.Utf8.parse(key)  // IV = key (transport convention)

  const encrypted = CryptoJS.AES.encrypt(
    CryptoJS.enc.Utf8.parse(plainText),
    keyBytes,
    {
      iv: ivBytes,
      mode: CryptoJS.mode.CFB,
      padding: CryptoJS.pad.NoPadding,
    },
  )
  return encrypted.toString()  // Base64
}
