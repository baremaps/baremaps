/*
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baremaps.shapefile;



import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Reader of a Database Binary content.
 *
 * @author Marc Le Bihan
 */
public class DbaseByteReader extends CommonByteReader implements AutoCloseable {

  /** First data record position, in bytes. */
  protected short firstRecordPosition;
  /** Size of one record, in bytes. */
  protected short recordLength;
  /** Reserved (dBASE IV) Filled with 00h. */
  protected byte[] reservedFiller1 = new byte[2];
  /**
   * Reserved : Incomplete transaction (dBASE IV). 00h : Transaction ended (or rolled back). 01h :
   * Transaction started.
   */
  protected byte reservedIncompleteTransaction;
  /** Reserved : Encryption flag (dBASE IV). 00h : Not encrypted. 01h : Data encrypted. */
  protected byte reservedEncryptionFlag;
  /** Reserved : Free record thread (for LAN only). */
  protected byte[] reservedFreeRecordThread = new byte[4];
  /** Reserved : For multi-user (DBase 3+). */
  protected byte[] reservedMultiUser = new byte[8];
  /** Reserved : MDX flag (dBASE IV). */
  protected byte reservedMDXFlag;
  /** Binary code page value. */
  protected byte codePage;
  /** Reserved (dBASE IV) Filled with 00h. */
  protected byte[] reservedFiller2 = new byte[2];
  /** Marks the end of the descriptor : must be 0x0D. */
  protected byte descriptorTerminator;
  /** Valid dBASE III PLUS table file (03h without a memo .DBT file; 83h with a memo). */
  protected byte dbaseVersion;
  /** Number of records in the table. */
  protected int rowCount;
  /** Database charset. */
  protected Charset charset;
  /** Date of last update; in YYMMDD format. */
  protected byte[] dbaseLastUpdate = new byte[3];
  /** List of field descriptors. */
  private List<DBaseFieldDescriptor> fieldsDescriptors = new ArrayList<>();

  /** Connection properties. */
  private Properties info;

  /**
   * Construct a mapped byte reader on a file.
   *
   * @param dbase3File File.
   * @param connectionInfos Connection properties, maybe null.
   * @throws DbaseException if the database seems to be invalid.
   */
  public DbaseByteReader(File dbase3File, Properties connectionInfos) throws IOException {
    super(dbase3File);
    this.info = connectionInfos;

    // React to special features asked.
    if (this.info != null) {
      // Sometimes, DBF files have a wrong charset, or more often : none, and you have to specify
      // it.
      String recordCharset = (String) this.info.get("record_charset");

      if (recordCharset != null) {
        Charset cs = Charset.forName(recordCharset);
        setCharset(cs);
      }
    }

    loadDescriptor();
  }

  /**
   * Load a row into a feature.
   *
   * @param row Feature to fill.
   */
  public void loadRow(List<Object> row) {
    // TODO: ignore deleted records
    getByteBuffer().get(); // denotes whether deleted or current
    // read first part of record

    var check = nextRowAvailable();

    for (DBaseFieldDescriptor fieldDescriptor : this.fieldsDescriptors) {
      byte[] data = new byte[fieldDescriptor.getLength()];
      getByteBuffer().get(data);

      int length = data.length;
      while (length != 0 && Byte.toUnsignedInt(data[length - 1]) <= ' ') {
        length--;
      }

      String value = new String(data, 0, length);

      // TODO: move somewhere else
      Object object = switch (fieldDescriptor.getType()) {
        case CHARACTER -> value;
        case NUMBER -> getNumber(fieldDescriptor, value);
        case CURRENCY -> Double.parseDouble(value.trim());
        case INTEGER -> Integer.parseInt(value.trim());
        case DOUBLE -> Double.parseDouble(value.trim());
        case AUTO_INCREMENT -> Integer.parseInt(value.trim());
        case LOGICAL -> value;
        case DATE -> {
          if (value.trim().isEmpty()) {
            yield null;
          } else {
            // Convert date string to LocalDate
            // DBF date format is YYYYMMDD
            try {
              yield LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e) {
              // If parsing fails, return the original string
              yield value;
            }
          }
        }
        case MEMO -> value;
        case FLOATING_POINT -> value;
        case PICTURE -> value;
        case VARI_FIELD -> value;
        case VARIANT -> value;
        case TIMESTAMP -> value;
        case DATE_TIME -> value;
      };

      row.add(object);
    }
  }

  private Object getNumber(DBaseFieldDescriptor fd, String value) {
    if (fd.getDecimalCount() == 0) {
      return Long.parseLong(value.trim());
    } else {
      return Double.parseDouble(value.trim());
    }
  }

  /**
   * Checks if a next row is available. Warning : it may be a deleted one.
   *
   * @return true if a next row is available.
   */
  public boolean nextRowAvailable() {
    // 1) Check for remaining bytes.
    if (getByteBuffer().hasRemaining() == false) {
      return false;
    }

    // 2) Check that the immediate next byte read isn't the EOF signal.
    byte eofCheck = getByteBuffer().get();

    if (eofCheck == 0x1A) {
      return false;
    } else {
      // Return one byte back.
      int position = getByteBuffer().position();
      getByteBuffer().position(position - 1);
      return true;
    }
  }

  /**
   * Returns the record number of the last record red.
   *
   * @return The record number.
   */
  public int getRowNum() {
    int position = getByteBuffer().position();
    return (position - Short.toUnsignedInt(firstRecordPosition))
        / Short.toUnsignedInt(recordLength);
  }

  /**
   * Read the next row as a set of objects.
   *
   * @return Map of field name / object value.
   */
  public Map<String, byte[]> readNextRowAsObjects() {
    // TODO: ignore deleted records
    /* byte isDeleted = */ getByteBuffer().get(); // denotes whether deleted or current

    // read first part of record
    HashMap<String, byte[]> fieldsValues = new HashMap<>();

    for (DBaseFieldDescriptor fd : this.fieldsDescriptors) {
      byte[] data = new byte[fd.getLength()];
      getByteBuffer().get(data);

      // Trim the bytes right.
      int length = data.length;

      while (length != 0 && Byte.toUnsignedInt(data[length - 1]) <= ' ') {
        length--;
      }

      if (length != data.length) {
        byte[] dataTrimmed = new byte[length];

        for (int index = 0; index < length; index++) {
          dataTrimmed[index] = data[index];
        }

        fieldsValues.put(fd.getName(), dataTrimmed);
      } else {
        fieldsValues.put(fd.getName(), data);
      }
    }

    return fieldsValues;
  }

  /**
   * Loading the database file content from binary .dbf file.
   *
   * @throws IOException if descriptor is not readable.
   */
  private void loadDescriptor() throws IOException {
    try {
      this.dbaseVersion = getByteBuffer().get();
      getByteBuffer().get(this.dbaseLastUpdate);

      getByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
      this.rowCount = getByteBuffer().getInt();
      this.firstRecordPosition = getByteBuffer().getShort();
      this.recordLength = getByteBuffer().getShort();
      getByteBuffer().order(ByteOrder.BIG_ENDIAN);

      getByteBuffer().get(this.reservedFiller1);
      this.reservedIncompleteTransaction = getByteBuffer().get();
      this.reservedEncryptionFlag = getByteBuffer().get();
      getByteBuffer().get(this.reservedFreeRecordThread);
      getByteBuffer().get(this.reservedMultiUser);
      this.reservedMDXFlag = getByteBuffer().get();

      // Translate code page value to a known charset.
      this.codePage = getByteBuffer().get();

      if (this.charset == null) {
        try {
          this.charset = toCharset(this.codePage);
        } catch (UnsupportedCharsetException e) {
          // Warn the caller that he will have to perform is own conversions.
        }
      }

      getByteBuffer().get(this.reservedFiller2);

      while (getByteBuffer().position() < this.firstRecordPosition - 1) {
        DBaseFieldDescriptor fd = new DBaseFieldDescriptor(getByteBuffer());
        this.fieldsDescriptors.add(fd);
        // loop until you hit the 0Dh field terminator
      }

      this.descriptorTerminator = getByteBuffer().get();

      // If the last character read after the field descriptor isn't 0x0D, the expected mark has not
      // been found and the DBF is corrupted.
      if (this.descriptorTerminator != 0x0D) {
        throw new DbaseException("File descriptor problem");
      }
    } catch (BufferUnderflowException e) {
      // This exception doesn't denote a trouble of file opening because the file has been checked
      // before
      // the calling of this private function.
      // Therefore, an internal structure problem cause maybe a premature End of file or anything
      // else, but the only thing
      // we can conclude is : we are not before a device trouble, but a file format trouble.
      throw new DbaseException("File descriptor problem");
    }
  }

  /**
   * Returns the fields descriptors in their binary format.
   *
   * @return Fields descriptors.
   */
  public List<DBaseFieldDescriptor> getFieldsDescriptors() {
    return this.fieldsDescriptors;
  }

  /**
   * Returns the charset.
   *
   * @return Charset.
   */
  public Charset getCharset() {
    return this.charset;
  }

  /**
   * Returns the database last update date.
   *
   * @return Date of the last update.
   */
  public Date getDateOfLastUpdate() {
    return toDate(this.dbaseLastUpdate);
  }

  /**
   * Returns the first record position, in bytes, in the DBase file.
   *
   * @return First record position.
   */
  public short getFirstRecordPosition() {
    return this.firstRecordPosition;
  }

  /**
   * Returns the length (in bytes) of one record in this DBase file, including the delete flag.
   *
   * @return Record length.
   */
  public short getRecordLength() {
    return this.recordLength;
  }

  /**
   * Returns the record count.
   *
   * @return Record count.
   */
  public int getRowCount() {
    return this.rowCount;
  }

  /**
   * Convert the binary code page value of the Dbase 3 file to a recent Charset.
   *
   * @param codePageBinaryValue page code binary value.
   * @return Charset.
   * @throws UnsupportedCharsetException if the code page as no representation in recents Charset
   *         (legacy DOS or macintosh charsets).
   */
  protected Charset toCharset(byte codePageBinaryValue) throws UnsupportedCharsetException {
    // Attempt to find a known conversion.
    String dbfCodePage = toCodePage(codePageBinaryValue);

    // If the code page cannot find a match for a more recent Charset, we wont be able to handle
    // this DBF.
    if (dbfCodePage == null) {
      throw new UnsupportedCharsetException("Unsupported codepage");
    }

    try {
      return Charset.forName(dbfCodePage);
    } catch (IllegalArgumentException e) {
      // If this happens here, it means that we have selected a wrong charset. We have a bug.
      throw new RuntimeException("Wrong charset selection");
    }
  }

  /**
   * Return a Charset code page from a binary code page value.
   *
   * @param pageCodeBinaryValue binary code page value.
   * @return Page code.
   */
  @SuppressWarnings("squid:S1192")
  private String toCodePage(byte pageCodeBinaryValue) {
    // From http://trac.osgeo.org/gdal/ticket/2864
    HashMap<Integer, String> knownConversions = new HashMap<>();
    knownConversions.put(0x01, "cp437"); // U.S. MS–DOS
    knownConversions.put(0x02, "cp850"); // International MS–DOS
    knownConversions.put(0x03, "cp1252"); // Windows ANSI
    knownConversions.put(0x08, "cp865"); // Danish OEM
    knownConversions.put(0x09, "cp437"); // Dutch OEM
    knownConversions.put(0x0a, "cp850"); // Dutch OEM*
    knownConversions.put(0x0b, "cp437"); // Finnish OEM
    knownConversions.put(0x0d, "cp437"); // French OEM
    knownConversions.put(0x0e, "cp850"); // French OEM*
    knownConversions.put(0x0f, "cp437"); // German OEM
    knownConversions.put(0x10, "cp850"); // German OEM*
    knownConversions.put(0x11, "cp437"); // Italian OEM
    knownConversions.put(0x12, "cp850"); // Italian OEM*
    knownConversions.put(0x13, "cp932"); // Japanese Shift-JIS
    knownConversions.put(0x14, "cp850"); // Spanish OEM*
    knownConversions.put(0x15, "cp437"); // Swedish OEM
    knownConversions.put(0x16, "cp850"); // Swedish OEM*
    knownConversions.put(0x17, "cp865"); // Norwegian OEM
    knownConversions.put(0x18, "cp437"); // Spanish OEM
    knownConversions.put(0x19, "cp437"); // English OEM (Britain)
    knownConversions.put(0x1a, "cp850"); // English OEM (Britain)*
    knownConversions.put(0x1b, "cp437"); // English OEM (U.S.)
    knownConversions.put(0x1c, "cp863"); // French OEM (Canada)
    knownConversions.put(0x1d, "cp850"); // French OEM*
    knownConversions.put(0x1f, "cp852"); // Czech OEM
    knownConversions.put(0x22, "cp852"); // Hungarian OEM
    knownConversions.put(0x23, "cp852"); // Polish OEM
    knownConversions.put(0x24, "cp860"); // Portuguese OEM
    knownConversions.put(0x25, "cp850"); // Portuguese OEM*
    knownConversions.put(0x26, "cp866"); // Russian OEM
    knownConversions.put(0x37, "cp850"); // English OEM (U.S.)*
    knownConversions.put(0x40, "cp852"); // Romanian OEM
    knownConversions.put(0x4d, "cp936"); // Chinese GBK (PRC)
    knownConversions.put(0x4e, "cp949"); // Korean (ANSI/OEM)
    knownConversions.put(0x4f, "cp950"); // Chinese Big5 (Taiwan)
    knownConversions.put(0x50, "cp874"); // Thai (ANSI/OEM)
    knownConversions.put(0x57, "cp1252"); // ANSI
    knownConversions.put(0x58, "cp1252"); // Western European ANSI
    knownConversions.put(0x59, "cp1252"); // Spanish ANSI
    knownConversions.put(0x64, "cp852"); // Eastern European MS–DOS
    knownConversions.put(0x65, "cp866"); // Russian MS–DOS
    knownConversions.put(0x66, "cp865"); // Nordic MS–DOS
    knownConversions.put(0x67, "cp861"); // Icelandic MS–DOS
    knownConversions.put(0x6a, "cp737"); // Greek MS–DOS (437G)
    knownConversions.put(0x6b, "cp857"); // Turkish MS–DOS
    knownConversions.put(0x6c, "cp863"); // French–Canadian MS–DOS
    knownConversions.put(0x78, "cp950"); // Taiwan Big 5
    knownConversions.put(0x79, "cp949"); // Hangul (Wansung)
    knownConversions.put(0x7a, "cp936"); // PRC GBK
    knownConversions.put(0x7b, "cp932"); // Japanese Shift-JIS
    knownConversions.put(0x7c, "cp874"); // Thai Windows/MS–DOS
    knownConversions.put(0x86, "cp737"); // Greek OEM
    knownConversions.put(0x87, "cp852"); // Slovenian OEM
    knownConversions.put(0x88, "cp857"); // Turkish OEM
    knownConversions.put(0xc8, "cp1250"); // Eastern European Windows
    knownConversions.put(0xc9, "cp1251"); // Russian Windows
    knownConversions.put(0xca, "cp1254"); // Turkish Windows
    knownConversions.put(0xcb, "cp1253"); // Greek Windows
    knownConversions.put(0xcc, "cp1257"); // Baltic Windows

    return (knownConversions.get(Byte.toUnsignedInt(pageCodeBinaryValue)));
  }

  /**
   * Set a charset.
   *
   * @param cs Charset.
   */
  public void setCharset(Charset cs) {
    this.charset = cs;
  }

  /**
   * Return a date from a byte array.
   *
   * @param yymmdd byte[3] with byte[0] = year (2 digits), [1] = month, [2] = day.
   * @return Date.
   */
  private Date toDate(byte[] yymmdd) {
    Objects.requireNonNull(yymmdd, "the yymmdd bytes cannot be null");

    if (yymmdd.length != 3) {
      throw new IllegalArgumentException(MessageFormat.format(
          "Database:toDate() works only on a 3 bytes YY MM DD date. this array has {0} length",
          yymmdd.length));
    }

    Objects.requireNonNull(yymmdd[0], "the year byte cannot be null");
    Objects.requireNonNull(yymmdd[1], "the month byte cannot be null");
    Objects.requireNonNull(yymmdd[2], "the day byte cannot be null");

    int year = yymmdd[0] < 70 ? 100 + yymmdd[0] : yymmdd[0];
    int month = yymmdd[1];
    int day = yymmdd[2];

    @SuppressWarnings("deprecation") // But everything is deprecated in DBF files...
    Date date = new Date(year, month, day);
    return date;
  }
}
