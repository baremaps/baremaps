/**
 Licensed under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/
// Based on https://www.irr.net/docs/list.html
const nics = [
    {url: "https://ftp.afrinic.net/pub/dbase/afrinic.db.gz", filename: "afrinic.db"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.as-block.gz", filename: "apnic.db.as-block.db"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.as-set.gz", filename: "apnic.db.as-set.db"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.domain.gz", filename: "apnic.db.domain"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.filter-set.gz", filename: "apnic.db.filter-set"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.inet-rtr.gz", filename: "apnic.db.inet-rtr"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.inet6num.gz", filename: "apnic.db.inet6num"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.inetnum.gz", filename: "apnic.db.inetnum"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.irt.gz", filename: "apnic.db.irt"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.key-cert.gz", filename: "apnic.db.key-cert"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.limerick.gz", filename: "apnic.db.limerick"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.mntner.gz", filename: "apnic.db.mntner"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.organisation.gz", filename: "apnic.db.organisation"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.peering-set.gz", filename: "apnic.db.peering-set"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.role.gz", filename: "apnic.db.role"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.route-set.gz", filename: "apnic.db.route-set"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.route.gz", filename: "apnic.db.route"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.route6.gz", filename: "apnic.db.route6"},
    {url: "https://ftp.apnic.net/apnic/whois/apnic.db.rtr-set.gz", filename: "apnic.db.rtr-set"},
    {url: "https://ftp.arin.net/pub/rr/arin.db.gz", filename: "arin.db"},
    {url: "https://ftp.lacnic.net/lacnic/dbase/lacnic.db.gz", filename: "lacnic.db"},
    {url: "https://ftp.ripe.net/ripe/dbase/ripe.db.gz", filename: "ripe.db"},
    {url: "ftp://ftp.altdb.net/pub/altdb/altdb.db.gz", filename: "altdb.db"},
    {url: "ftp://whois.in.bell.ca/bell.db.gz", filename: "bell.db"},
    {url: "ftp://irr.bboi.net/bboi.db.gz", filename: "bboi.db"},
    {url: "https://whois.canarie.ca/dbase/canarie.db.gz", filename: "canarie.db"},
    {url: "ftp://irr-mirror.idnic.net/idnic.db.gz", filename: "idnic.db"},
    {url: "ftp://ftp.nic.ad.jp/jpirr/jpirr.db.gz", filename: "jpirr.db"},
    {url: "ftp://rr.Level3.net/level3.db.gz", filename: "level3.db"},
    {url: "ftp://ftp.nestegg.net/irr/nestegg.db.gz", filename: "nestegg.db"},
    {url: "ftp://rr1.ntt.net/nttcomRR/nttcom.db.gz", filename: "nttcom.db"},
    {url: "ftp://ftp.panix.com/pub/rrdb/panix.db.gz", filename: "panix.db"},
    {url: "ftp://ftp.radb.net/radb/dbase/radb.db.gz", filename: "radb.db"},
    {url: "ftp://ftp.radb.net/radb/dbase/reach.db.gz", filename: "reach.db"},
    {url: "ftp://ftp.bgp.net.br/tc.db.gz", filename: "tc.db"}
];

export default {"steps": [
    {
        id: `iploc`,
        needs: [],
        tasks: [
            ...nics.map(nic => {
                return {
                    type: "DownloadUrl",
                    source: nic.url,
                    target: `downloads/${nic.filename}.gz`,
                    replaceExisting: true
                }
            }),
            ...nics.map(nic => {
                return {
                    type: "DecompressFile",
                    source: `downloads/${nic.filename}.gz`,
                    target: `archives/${nic.filename}`,
                    compression: "gzip"
                }
            }),
            {
                type: "DownloadUrl",
                source: "https://download.geonames.org/export/dump/allCountries.zip",
                target: "downloads/geonames-allcountries.zip",
                replaceExisting: true
            },
            {
                type: "DecompressFile",
                source: "downloads/geonames-allcountries.zip",
                target: "archives",
                compression: "zip"
            },
            {
                type: "CreateGeonamesIndex",
                dataFile: "archives/allCountries.txt",
                indexDirectory: "geocoder-index"
            },
            {
                type: "CreateIplocIndex",
                geonamesIndexPath: "geocoder-index",
                nicPaths: nics.map(nic => `archives/${nic.filename}`),
                targetIplocIndexPath: "iploc.db"
            }
        ]
    }
]};
