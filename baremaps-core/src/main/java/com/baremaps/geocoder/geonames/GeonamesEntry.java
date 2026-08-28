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

package com.baremaps.geocoder.geonames;

/**
 * A record from the Geonames database.
 */
public class GeonamesEntry {

  // integer id of record in geonames database
  private Integer geonameid;

  // name of geographical point (utf8) varchar(200)
  private String name;

  // name of geographical point in plain ascii characters, varchar(200)
  private String asciiname;

  // alternatenames, comma separated, ascii names automatically transliterated,
  // convenience attribute from alternatename table, varchar(10000)
  private String alternatenames;

  // latitude in decimal degrees (wgs84)
  private Double latitude;

  // longitude in decimal degrees (wgs84)
  private Double longitude;

  // see http://www.geonames.org/export/codes.html, char(1)
  private String featureClass;

  // see http://www.geonames.org/export/codes.html, varchar(10)
  private String featureCode;

  // ISO-3166 2-letter country code, 2 characters
  private String countryCode;

  // alternate country codes, comma separated, ISO-3166 2-letter country code, 200 characters
  private String cc2;

  // fipscode (subject to change to iso code), see exceptions below, see file
  // admin1Codes.txt for display names of this code; varchar(20)
  // in switzerland usually canton code (ex: VD)
  private String admin1Code;

  // code for the second administrative division, a county in the US, see file
  // admin2Codes.txt; varchar(80)
  private String admin2Code;

  // code for third level administrative division, varchar(20)
  private String admin3Code;

  // code for fourth level administrative division, varchar(20)
  private String admin4Code;

  // population
  private Long population;

  // elevation in meters, integer
  private Integer elevation;

  // digital elevation model, srtm3 or gtopo30, average elevation of 3''x3'' (ca 90mx90m) or
  // 30''x30'' (ca 900mx900m) area in meters, integer. srtm processed by cgiar/ciat.
  private Integer dem;

  // the iana timezone id (see file timeZone.txt) varchar(40)
  private String timezone;

  // date of last modification in yyyy-MM-dd format
  private String modificationDate;

  public GeonamesEntry() {
    // Default constructor
  }

  public Integer getGeonameid() {
    return geonameid;
  }

  public void setGeonameid(Integer geonameid) {
    this.geonameid = geonameid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAsciiname() {
    return asciiname;
  }

  public void setAsciiname(String asciiname) {
    this.asciiname = asciiname;
  }

  public String getAlternatenames() {
    return alternatenames;
  }

  public void setAlternatenames(String alternatenames) {
    this.alternatenames = alternatenames;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public String getFeatureClass() {
    return featureClass;
  }

  public void setFeatureClass(String featureClass) {
    this.featureClass = featureClass;
  }

  public String getFeatureCode() {
    return featureCode;
  }

  public void setFeatureCode(String featureCode) {
    this.featureCode = featureCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public String getCc2() {
    return cc2;
  }

  public void setCc2(String cc2) {
    this.cc2 = cc2;
  }

  public String getAdmin1Code() {
    return admin1Code;
  }

  public void setAdmin1Code(String admin1Code) {
    this.admin1Code = admin1Code;
  }

  public String getAdmin2Code() {
    return admin2Code;
  }

  public void setAdmin2Code(String admin2Code) {
    this.admin2Code = admin2Code;
  }

  public String getAdmin3Code() {
    return admin3Code;
  }

  public void setAdmin3Code(String admin3Code) {
    this.admin3Code = admin3Code;
  }

  public String getAdmin4Code() {
    return admin4Code;
  }

  public void setAdmin4Code(String admin4Code) {
    this.admin4Code = admin4Code;
  }

  public Long getPopulation() {
    return population;
  }

  public void setPopulation(Long population) {
    this.population = population;
  }

  public Integer getElevation() {
    return elevation;
  }

  public void setElevation(Integer elevation) {
    this.elevation = elevation;
  }

  public Integer getDem() {
    return dem;
  }

  public void setDem(Integer dem) {
    this.dem = dem;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public String getModificationDate() {
    return modificationDate;
  }

  public void setModificationDate(String modificationDate) {
    this.modificationDate = modificationDate;
  }
}
