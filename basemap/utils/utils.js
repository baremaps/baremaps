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

export function withSortKeys(directives) {
    return directives
        .map(withFillSortKey)
        .map(withLineSortKey);
}

export function withFillSortKey(directive, index, array) {
    return directive['fill-color'] ?{
        ...directive,
        'fill-sort-key': array.length - index,
    } : directive;
}

export function withLineSortKey(directive, index, array) {
    return directive['line-width'] || directive['line-width-stops'] ? {
        ...directive,
        'line-sort-key': array.length - index,
    } : directive;
}

export function withSymbolSortKeys(directives) {
    return directives.map(withSymbolSortKey);
}

export function withSymbolSortKey(directive, index) {
    return directive['symbol-sort-key'] ? directive : {
        ...directive,
        'symbol-sort-key': index,
    };
}

export function asLayerObject(directives = [], baseLayer = {}) {
    return {
        ...baseLayer,
        filter: asFilterProperty(directives, baseLayer['filter']),
        layout: asLayoutProperty(directives, baseLayer['layout']),
        paint: asPaintProperty(directives, baseLayer['paint']),
    };
}

export function asLayoutProperty(directives = [], baseLayout = {}) {
    return Object.assign(
        {
            ...textFont(directives),
            ...textField(directives),
            ...textSize(directives),
            ...textSizeStops(directives),
            ...textMaxWidth(directives),
            ...iconImage(directives),
            ...lineSortKey(directives),
            ...fillSortKey(directives),
            ...symbolSortKey(directives),
        },
        baseLayout,
    )
}

export function asPaintProperty(directives = [], basePaint = {}) {
    return Object.assign(
        {
            ...textColor(directives),
            ...textHaloColor(directives),
            ...textHaloWidth(directives),
            ...iconColor(directives),
            ...fillColor(directives),
            ...fillOutlineColor(directives),
            ...lineColor(directives),
            ...lineWidth(directives),
            ...lineWidthStops(directives),
            ...lineGapWidth(directives),
            ...lineGapWidthStops(directives),
            ...textColor(directives),
        },
        basePaint,
    )
}

export function asFilterProperty(directives = [], filter = []) {
    if (directives.length > 0 && filter.length > 0) {
        return [
            'all',
            filter,
            ['any', ...directives.map((rule) => rule['filter'])],
        ];
    } else if (directives.length > 0) {
        return ['any', ...directives.map((rule) => rule['filter'])];
    } else if (filter.length > 0) {
        return ['all', filter];
    } else {
        return [];
    }
}

function iconImage(directives) {
    return mergeDirectives(directives, 'icon-image', 'none')
}

function iconColor(directives) {
    return mergeDirectives(directives, 'icon-color', 'rgba(0, 0, 0, 0)')
}

function textFont(directives) {
    return mergeDirectives(directives, 'text-font', "Arial")
}

function textField(directives) {
    return mergeDirectives(directives, 'text-field', null)
}

function textMaxWidth(directives) {
    return mergeDirectives(directives, 'text-max-width', 4)
}

function textColor(directives) {
    return mergeDirectives(directives, 'text-color', 'rgba(0, 0, 0, 0)')
}

function textHaloColor(directives) {
    return mergeDirectives(directives, 'text-halo-color', 'rgba(0, 0, 0, 0)')
}

function textHaloWidth(directives) {
    return mergeDirectives(directives, 'text-halo-width', 0)
}

function fillColor(directives) {
    return mergeDirectives(directives, 'fill-color', 'rgba(0, 0, 0, 0)')
}

function fillOutlineColor(directives) {
    return mergeDirectives(directives, 'fill-outline-color', 'rgba(0, 0, 0, 0)')
}

function lineColor(directives) {
    return mergeDirectives(directives, 'line-color', 'rgba(0, 0, 0, 0)')
}

function lineWidth(directives) {
    return mergeDirectives(directives, 'line-width', 0)
}

function lineGapWidth(directives) {
    return mergeDirectives(directives, 'line-gap-width', 0)
}

function lineSortKey(directives) {
    return mergeDirectives(directives, 'line-sort-key', 0)
}

function fillSortKey(directives) {
    return mergeDirectives(directives, 'fill-sort-key', 0)
}

function symbolSortKey(directives) {
    return mergeDirectives(directives, 'symbol-sort-key', 0)
}

function textSize(directives) {
    return mergeDirectives(directives, 'text-size', 0)
}

function mergeDirectives(directives, property, value) {
    let cases = directives.flatMap((rule) => {
        if (rule[property]) {
            return [rule['filter'], rule[property]]
        } else {
            return []
        }
    })
    if (cases.length == 0) {
        return {}
    }
    return {
        [property]: ['case', ...cases, value],
    }
}

function lineWidthStops(directives) {
    return interpolateStops(directives, 'line-width-stops', 'line-width', 1)
}

function lineGapWidthStops(directives) {
    return interpolateStops(directives, 'line-gap-width-stops', 'line-gap-width', 1)
}

function textSizeStops(directives) {
    return interpolateStops(directives, 'text-size-stops', 'text-size', 1)
}

function interpolateStops(directives, property, alias, value) {
    if (directives.filter((directive) => directive[property]).length == 0) {
        return {};
    }
    var mergedDirective = [
        'interpolate',
        ['linear'],
        ['zoom'],
    ];
    for (let zoom = 0; zoom <= 22; zoom++) {
        mergedDirective.push(zoom);
        let cases = ['case']
        for (let directive of directives) {
            if (directive[property]) {
                let filter = directive['filter'];
                let value = interpolate(zoom, directive[property]);
                cases.push(filter);
                cases.push(value);
            }
        }
        cases.push(value);
        mergedDirective.push(cases);
    }
    return {
        [alias]: mergedDirective,
    }
}

/**
 * Group an array of objects by a given key.
 *
 * @param xs
 * @param key
 * @returns {*}
 */
function groupBy(xs, key) {
    return xs.reduce(function (rv, x) {
        ;(rv[x[key]] = rv[x[key]] || []).push(x)
        return rv
    }, {})
}

/**
 * Given an array in the form of [zoom_level_n, value_n, zoom_level_m, value_m, ...], with n < m, n >= 0, and m <= 22,
 * the function linearly interpolates the value for the given zoom level.
 *
 * The values before zoom_level_n are assumed to be equal to 0.
 * The values after zoom_level_m are assumed to be equal to value_m * 2 ** (zoom - zoom_level_m).
 *
 * Here are a few examples:
 * interpolate(0, [10, 1, 14, 5]) = 0
 * interpolate(9, [10, 1, 14, 5]) = 0
 * interpolate(10, [10, 1, 14, 5]) = 1
 * interpolate(11, [10, 1, 14, 5]) = 2
 * interpolate(12, [10, 1, 14, 5]) = 3
 * interpolate(13, [10, 1, 14, 5]) = 4
 * interpolate(14, [10, 1, 14, 5]) = 5
 * interpolate(15, [10, 1, 14, 5]) = 10
 * interpolate(17, [10, 1, 14, 5]) = 40
 * interpolate(22, [10, 1, 14, 5]) = 5
 */
export function interpolate(zoom, values) {
    let i = 0
    while (i < values.length && zoom >= values[i]) {
        i += 2;
    }
    if (i >= values.length) {
        return values[values.length - 1] * 2 ** (zoom - values[values.length - 2]);
    }
    if (i === 0) {
        return 0;
    }
    const zoomN = values[i - 2];
    const valueN = values[i - 1];
    const zoomM = values[i];
    const valueM = values[i + 1];
    return valueN + (valueM - valueN) * (zoom - zoomN) / (zoomM - zoomN);
}

