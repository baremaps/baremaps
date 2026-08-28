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

    grayscale() {
        let rgb = this.toRGB();
        let gray = Math.round(0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b);
        return new RGB(gray, gray, gray, rgb.a);
    }

    lighten(amount) {
        let hsl = this.toHSL();
        hsl.l += amount;
        hsl.l = Math.min(hsl.l, 1);
        return hsl.toRGB();
    }

    darken(amount) {
        let hsl = this.toHSL();
        hsl.l -= amount;
        hsl.l = Math.max(hsl.l, 0);
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

    contrast(factor) {
        factor = (1 + factor) ** 2;
        let rgb = this.toRGB();
        let r = ((rgb.r / 255.0 - 0.5) * factor + 0.5) * 255.0;
        let g = ((rgb.g / 255.0 - 0.5) * factor + 0.5) * 255.0;
        let b = ((rgb.b / 255.0 - 0.5) * factor + 0.5) * 255.0;
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return new RGB(r, g, b, rgb.a);
    }

    colorblind(type) {
        return type(this.toRGB());
    }

    toString() {
        throw new Error('Abstract method');
    }
}

class RGB extends Color {

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

    toString() {
        if (this.a == 1) {
            return `rgb(${this.r},${this.g},${this.b})`;
        } else {
            return `rgba(${this.r},${this.g},${this.b},${this.a})`;
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

function Achromatopsia(rgb) {
    let r = rgb.r * 0.299 + rgb.g * 0.587 + rgb.b * 0.114;
    let g = rgb.r * 0.299 + rgb.g * 0.587 + rgb.b * 0.114;
    let b = rgb.r * 0.299 + rgb.g * 0.587 + rgb.b * 0.114;
    return new RGB(r, g, b, rgb.a);
}

function Achromatomaly(rgb) {
    let r = rgb.r * 0.618 + rgb.g * 0.320 + rgb.b * 0.062;
    let g = rgb.r * 0.163 + rgb.g * 0.775 + rgb.b * 0.062;
    let b = rgb.r * 0.163 + rgb.g * 0.320 + rgb.b * 0.516;
    return new RGB(r, g, b, rgb.a);
}

function Deuteranopia(rgb) {
    let r = rgb.r * 0.625 + rgb.g * 0.375 + rgb.b * 0.0;
    let g = rgb.r * 0.7 + rgb.g * 0.3 + rgb.b * 0.0;
    let b = rgb.r * 0.0 + rgb.g * 0.3 + rgb.b * 0.7;
    return new RGB(r, g, b, rgb.a);
}

function Deuteranomaly(rgb) {
    let r = rgb.r * 0.8 + rgb.g * 0.2 + rgb.b * 0.0;
    let g = rgb.r * 0.258 + rgb.g * 0.742 + rgb.b * 0.0;
    let b = rgb.r * 0.0 + rgb.g * 0.142 + rgb.b * 0.858;
    return new RGB(r, g, b, rgb.a);
}

function Protanopia(rgb) {
    let r = rgb.r * 0.567 + rgb.g * 0.433 + rgb.b * 0.0;
    let g = rgb.r * 0.558 + rgb.g * 0.442 + rgb.b * 0.0;
    let b = rgb.r * 0.0 + rgb.g * 0.242 + rgb.b * 0.758;
    return new RGB(r, g, b, rgb.a);
}

function Protanomaly(rgb) {
    let r = rgb.r * 0.817 + rgb.g * 0.183 + rgb.b * 0.0;
    let g = rgb.r * 0.333 + rgb.g * 0.667 + rgb.b * 0.0;
    let b = rgb.r * 0.0 + rgb.g * 0.125 + rgb.b * 0.875;
    return new RGB(r, g, b, rgb.a);
}


function Tritanopia(rgb) {
    let r = rgb.r * 0.95 + rgb.g * 0.05 + rgb.b * 0.0;
    let g = rgb.r * 0.0 + rgb.g * 0.433 + rgb.b * 0.567;
    let b = rgb.r * 0.0 + rgb.g * 0.475 + rgb.b * 0.525;
    return new RGB(r, g, b, rgb.a);
}

function Tritanomaly(rgb) {
    let r = rgb.r * 0.967 + rgb.g * 0.033 + rgb.b * 0.0;
    let g = rgb.r * 0.0 + rgb.g * 0.733 + rgb.b * 0.267;
    let b = rgb.r * 0.0 + rgb.g * 0.183 + rgb.b * 0.817;
    return new RGB(r, g, b, rgb.a);
}

const ColorBlind = {
    Achromatopsia: Achromatopsia,
    Achromatomaly: Achromatomaly,
    Deuteranopia: Deuteranopia,
    Deuteranomaly: Deuteranomaly,
    Protanopia: Protanopia,
    Protanomaly: Protanomaly,
    Tritanopia: Tritanopia,
    Tritanomaly: Tritanomaly
}

export {Color, RGB, HSL, ColorBlind};
