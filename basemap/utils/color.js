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

// The sRGB transfer function, IEC 61966-2-1, and the Rec.709 weights. Both are stated here rather
// than imported from `vision.js`, which imports this file: luminance is a property of a colour and
// the models of colour vision are built on it, not the other way round.
const LUMINANCE = [0.2126, 0.7152, 0.0722];

const toLinear = (value) => {
    const v = value / 255;
    return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
};

const fromLinear = (value) => {
    const v = Math.min(Math.max(value, 0), 1);
    return 255 * (v <= 0.0031308 ? v * 12.92 : 1.055 * v ** (1 / 2.4) - 0.055);
};

/**
 * How different two colours in CIELAB look, as CIEDE2000 measures it.
 *
 * Taken as two lab triples rather than as two colours, because the caller that measures a whole
 * palette against itself holds them already: converting inside would convert each colour once per
 * pair it is in, which for a palette of this size is a few hundred thousand conversions to do the
 * work of a few hundred.
 */
function differenceLab(one, two) {
        const c1 = Math.hypot(one.a, one.b);
        const c2 = Math.hypot(two.a, two.b);
        const mean = (c1 + c2) / 2;
        const g = 0.5 * (1 - Math.sqrt(mean ** 7 / (mean ** 7 + 25 ** 7)));
        const a1 = (1 + g) * one.a;
        const a2 = (1 + g) * two.a;
        const cp1 = Math.hypot(a1, one.b);
        const cp2 = Math.hypot(a2, two.b);
        const angle = (b, a) => {
            if (a === 0 && b === 0) return 0;
            const h = Math.atan2(b, a) * 180 / Math.PI;
            return h < 0 ? h + 360 : h;
        };
        const h1 = angle(one.b, a1);
        const h2 = angle(two.b, a2);
        const dl = two.l - one.l;
        const dc = cp2 - cp1;
        let dh = 0;
        if (cp1 * cp2 !== 0) {
            dh = h2 - h1;
            if (dh > 180) dh -= 360;
            else if (dh < -180) dh += 360;
        }
        const bigDh = 2 * Math.sqrt(cp1 * cp2) * Math.sin(dh * Math.PI / 360);
        const meanL = (one.l + two.l) / 2;
        const meanC = (cp1 + cp2) / 2;
        let meanH;
        if (cp1 * cp2 === 0) {
            meanH = h1 + h2;
        } else {
            meanH = (h1 + h2) / 2;
            if (Math.abs(h1 - h2) > 180) meanH += h1 + h2 < 360 ? 180 : -180;
        }
        const rad = (degrees) => degrees * Math.PI / 180;
        const t = 1 - 0.17 * Math.cos(rad(meanH - 30)) + 0.24 * Math.cos(rad(2 * meanH))
            + 0.32 * Math.cos(rad(3 * meanH + 6)) - 0.20 * Math.cos(rad(4 * meanH - 63));
        const sl = 1 + (0.015 * (meanL - 50) ** 2) / Math.sqrt(20 + (meanL - 50) ** 2);
        const sc = 1 + 0.045 * meanC;
        const sh = 1 + 0.015 * meanC * t;
        const rt = -Math.sin(rad(60 * Math.exp(-(((meanH - 275) / 25) ** 2))))
            * 2 * Math.sqrt(meanC ** 7 / (meanC ** 7 + 25 ** 7));
        return Math.sqrt((dl / sl) ** 2 + (dc / sc) ** 2 + (bigDh / sh) ** 2
            + rt * (dc / sc) * (bigDh / sh));
    }

class Color {

    constructor() {

    }

    static fromString(color) {
        let rgb = RGB.fromString(color);
        if (rgb != null) {
            return rgb;
        }
        let hsl = HSL.fromString(color);
        if (hsl != null) {
            return hsl;
        }
        return null;
    }

    toRGB() {
        throw new Error('Abstract method');
    }

    toHSL() {
        throw new Error('Abstract method');
    }

    /**
     * The relative luminance of the colour, as WCAG and Rec.709 define it: the light the three
     * primaries actually emit, weighted by how much of it the eye takes from each.
     *
     * On the light and not on the numbers encoding it. The two are a power of 2.4 apart, and a
     * weighted sum of the encoded numbers is not the luminance of anything: it was returning 0.63
     * for a colour whose luminance is 0.35, which is why colours that cleared the contrast floor
     * in a palette fell under it in the grey themes derived from that palette. Contrast is a ratio
     * of luminances, so a theme that computes the wrong grey cannot inherit its parent's contrast.
     */
    luminance() {
        const rgb = this.toRGB();
        return LUMINANCE[0] * toLinear(rgb.r)
            + LUMINANCE[1] * toLinear(rgb.g)
            + LUMINANCE[2] * toLinear(rgb.b);
    }

    /**
     * The colour in CIELAB, where a distance is something like a perceived difference and the
     * three axes are lightness, green against red, and blue against yellow.
     *
     * The last two are the axes a colour vision deficiency takes away, and the first is the one
     * every one of them leaves, which is why a palette drawn for such a reader is moved along it.
     */
    toLab() {
        const rgb = this.toRGB();
        const [r, g, b] = [toLinear(rgb.r), toLinear(rgb.g), toLinear(rgb.b)];
        // sRGB to CIEXYZ under D65, then XYZ to Lab, both as CIE 15 states them.
        const x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047;
        const y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b;
        const z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883;
        const f = (t) => t > 216 / 24389 ? Math.cbrt(t) : (841 / 108) * t + 4 / 29;
        const [fx, fy, fz] = [f(x), f(y), f(z)];
        return {l: 116 * fy - 16, a: 500 * (fx - fy), b: 200 * (fy - fz)};
    }

    /** The colour of this lightness, carrying the hue and the chroma this one has. */
    withLightness(lightness) {
        const {a, b} = this.toLab();
        return RGB.fromLab({l: lightness, a, b}, this.toRGB().a);
    }

    /**
     * How different this colour and another look, as CIEDE2000 measures it.
     *
     * A plain distance in CIELAB overstates the difference between two saturated colours and
     * understates it between two pale ones; CIEDE2000 is the correction the CIE published for
     * that, and it is what a threshold like "a reader cannot tell these apart" has to be read
     * against. Around 2.3 is the difference a person notices at all.
     *
     *   G. Sharma, W. Wu and E. N. Dalal, "The CIEDE2000 Color-Difference Formula", Color Research
     *   and Application 30(1), 21-30, 2005.
     */
    difference(other) {
        return differenceLab(this.toLab(), other.toLab());
    }

    /** The grey of the same luminance, which is what a colour is to an eye that reads no hue. */
    grayscale() {
        const gray = fromLinear(this.luminance());
        return new RGB(gray, gray, gray, this.toRGB().a);
    }

    /**
     * Lightens by a fraction of the distance left to white rather than by a fixed
     * amount of lightness, and darkens by a fraction of the distance left to black.
     *
     * A theme is a transform applied to every colour of the default one, so the
     * transform has to leave the colours as distinguishable as it found them. Adding
     * a constant does not: a map is mostly pale, and its background, its minor roads
     * and its landuse fills all sit above 0.9 lightness, so adding 0.1 to each of
     * them ran 43 of them into the clamp and out the other side as the same pure
     * white. The light theme lost the road network into its background, and the dark
     * theme, which inverts it, lost the same 43 into pure black.
     *
     * Scaling the remaining distance cannot reach either end, so no two colours ever
     * arrive at the same one. `lighten(0.1)` still reads as a tenth lighter, which is
     * what the caller asks for; it is a tenth of the room the colour has left.
     */
    lighten(amount) {
        let hsl = this.toHSL();
        hsl.l += amount * (1 - hsl.l);
        return hsl.toRGB();
    }

    darken(amount) {
        let hsl = this.toHSL();
        hsl.l -= amount * hsl.l;
        return hsl.toRGB();
    }

    saturate(amount) {
        let hsl = this.toHSL();
        hsl.s += amount;
        hsl.s = Math.min(hsl.s, 1);
        return hsl.toRGB();
    }

    desaturate(amount) {
        let hsl = this.toHSL();
        hsl.s -= amount;
        hsl.s = Math.max(hsl.s, 0);
        return hsl.toRGB();
    }

    fade(amount) {
        let rgb = this.toRGB();
        rgb.a -= amount;
        rgb.a = Math.max(rgb.a, 0);
        return rgb;
    }

    opacify(amount) {
        let rgb = this.toRGB();
        rgb.a += amount;
        rgb.a = Math.min(rgb.a, 1);
        return rgb;
    }

    rotate(degrees) {
        let hsl = this.toHSL();
        hsl.h = (hsl.h + degrees) % 360;
        return hsl.toRGB();
    }

    invert() {
        let rgb = this.toRGB();
        rgb.r = 255 - rgb.r;
        rgb.g = 255 - rgb.g;
        rgb.b = 255 - rgb.b;
        return rgb;
    }

    /**
     * Pushes each channel away from mid-grey along a curve that steepens the middle
     * of the scale and flattens towards its ends, rather than along the straight
     * line that has to be clipped where it leaves the scale.
     *
     * The straight line is the reason a contrast theme is worth less than it looks:
     * a map's paler colours are bunched near the top of the scale, so raising the
     * contrast sends them past white and the clip returns them all as white. The
     * background, the residential and living streets and the parking overlay came
     * back as one colour, which is the opposite of what the theme is asked for.
     *
     * `factor` keeps its meaning, as the slope the curve has at mid-grey, and the
     * curve reaches an end of the scale only for a channel that started there.
     */
    contrast(factor) {
        factor = (1 + factor) ** 2;
        let rgb = this.toRGB();
        let curve = (value) => {
            let v = value / 255.0;
            if (v <= 0 || v >= 1) {
                return value;
            }
            return 255.0 * v ** factor / (v ** factor + (1 - v) ** factor);
        };
        let r = curve(rgb.r);
        let g = curve(rgb.g);
        let b = curve(rgb.b);
        return new RGB(r, g, b, rgb.a);
    }

    toString() {
        throw new Error('Abstract method');
    }
}

class RGB extends Color {

    /** The colour a CIELAB triple names, clamped to what a display can show. */
    static fromLab({l, a, b}, alpha = 1) {
        const fy = (l + 16) / 116;
        const fx = fy + a / 500;
        const fz = fy - b / 200;
        const g = (t) => t ** 3 > 216 / 24389 ? t ** 3 : (t - 4 / 29) * (108 / 841);
        const x = g(fx) * 0.95047;
        const y = g(fy);
        const z = g(fz) * 1.08883;
        return new RGB(
            fromLinear(3.2404542 * x - 1.5371385 * y - 0.4985314 * z),
            fromLinear(-0.9692660 * x + 1.8760108 * y + 0.0415560 * z),
            fromLinear(0.0556434 * x - 0.2040259 * y + 1.0572252 * z),
            alpha);
    }

    constructor(r, g, b, a = 1) {
        super();
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    static fromString(color) {
        if (typeof color !== 'string') {
            return null;
        }
        let rgb = color.replace(/\s*/g, '').match(/rgb\((\d*)\,(\d*)\,(\d*)\)/)
        if (rgb != null) {
            return new RGB(parseInt(rgb[1]), parseInt(rgb[2]), parseInt(rgb[3]), 1)
        }
        let rgba = color.replace(/\s*/g, '').match(/rgba\((\d*)\,(\d*)\,(\d*)\,(.*)\)/)
        if (rgba != null) {
            return new RGB(parseInt(rgba[1]), parseInt(rgba[2]), parseInt(rgba[3]), parseFloat(rgba[4]))
        }
        return null
    }

    toRGB() {
        return this;
    }

    toHSL() {
        let r = this.r / 255;
        let g = this.g / 255;
        let b = this.b / 255;
        let max = Math.max(r, g, b);
        let min = Math.min(r, g, b);
        let h, s, l = (max + min) / 2;
        if (max == min) {
            h = s = 0;
        } else {
            let d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            switch (max) {
                case r:
                    h = (g - b) / d + (g < b ? 6 : 0)
                    break;
                case g:
                    h = (b - r) / d + 2
                    break;
                case b:
                    h = (r - g) / d + 4
                    break;
            }
            h /= 6;
        }
        return new HSL(h, s, l, this.a);
    }

    /**
     * Channels are rounded and clamped so that the result parses back through
     * `fromString`, whose pattern only accepts integers. Without this a theme
     * derived from another derived theme silently copies its parent, because
     * the colour-blindness matrices produce fractional channels and the failed
     * parse is indistinguishable from a value that is not a colour at all.
     */
    toString() {
        const channel = (value) => Math.min(255, Math.max(0, Math.round(value)));
        const r = channel(this.r), g = channel(this.g), b = channel(this.b);
        if (this.a == 1) {
            return `rgb(${r},${g},${b})`;
        } else {
            return `rgba(${r},${g},${b},${this.a})`;
        }
    }
}

class HSL extends Color {

    constructor(h, s, l, a = 1) {
        super();
        this.h = h;
        this.s = s;
        this.l = l;
        this.a = a;
    }

    static fromString(color) {
        if (typeof color !== 'string') {
            return null;
        }
        let hsl = color.replace(/\s*/g, '').match(/hsl\((\d*)\,(\d*)\,(\d*)\)/)
        if (hsl != null) {
            return new HSL(parseInt(hsl[1]), parseInt(hsl[2]), parseInt(hsl[3]), 1)
        }
        let hsla = color.replace(/\s*/g, '').match(/hsla\((\d*)\,(\d*)\,(\d*)\,(.*)\)/)
        if (hsla != null) {
            return new HSL(parseInt(hsla[1]), parseInt(hsla[2]), parseInt(hsla[3]), parseFloat(hsla[4]))
        }
        return null
    }

    toRGB() {
        let r, g, b;
        if (this.s == 0) {
            r = g = b = this.l;
        } else {
            let hue2rgb = function hue2rgb(p, q, t) {
                if (t < 0) {
                    t += 1;
                }
                if (t > 1) {
                    t -= 1;
                }
                if (t < 1 / 6) {
                    return p + (q - p) * 6 * t;
                }
                if (t < 1 / 2) {
                    return q;
                }
                if (t < 2 / 3) {
                    return p + (q - p) * (2 / 3 - t) * 6;
                }
                return p;
            }
            let q = this.l < 0.5 ? this.l * (1 + this.s) : this.l + this.s - this.l * this.s;
            let p = 2 * this.l - q;
            r = hue2rgb(p, q, this.h + 1 / 3);
            g = hue2rgb(p, q, this.h);
            b = hue2rgb(p, q, this.h - 1 / 3);
        }
        return new RGB(Math.round(r * 255), Math.round(g * 255), Math.round(b * 255), this.a);
    }

    toHSL() {
        return this;
    }

    toString() {
        if (this.a == 1) {
            return `hsl(${this.h},${this.s},${this.l})`;
        } else {
            return `hsla(${this.h},${this.s},${this.l},${this.a})`;
        }
    }
}

export {Color, RGB, HSL, differenceLab};
