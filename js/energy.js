var minYearEnergy = Number.MAX_SAFE_INTEGER;
var maxYearEnergy = 0;

var maxCo2 = 0;

var selectedYear = 2021;
var labels = ['Kohle', 'Gas', 'Öl', 'Nuklear', 'Hydro', 'Solar', 'Wind'];
var colors = {
    'Kohle': '#8B4513',
    'Gas': '#808080',
    'Öl': '#000000',
    'Nuklear': '#FF0000',
    'Hydro': '#0000FF',
    'Solar': '#FFFF00',
    'Wind': '#ADD8E6'
}; 

var energyLayer = new L.GeoJSON(null, {onEachFeature: onEachFeature});

googleSat = L.tileLayer('http://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
    maxZoom: 20,
    subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
    attribution: "<a href=\'http://maps.google.com/\'>Google</a> Maps Satellite",
    noWrap: true
});

googleStreets = L.tileLayer('http://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
    maxZoom: 20,
    subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
    attribution: "<a href=\'http://maps.google.com/\'>Google</a> Maps",
    noWrap: true
});


osmMapnik = new L.tileLayer(
    'http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    {
        maxZoom: 20,
        attribution: 'Map data © OpenStreetMap contributors'
    }
);

var map = L.map('mapid', {
    center: [30, 0],
    zoom: 2,
    layers: [osmMapnik, googleStreets, osmMapnik]
});

function updateYearSlider() {
    var yearSlider = document.getElementById('yearSlider');
    var minYearDisplay = document.getElementById('minYearCo2');
    var maxYearDisplay = document.getElementById('maxYearCo2');

    if (map.hasLayer(energyLayer)) {
        yearSlider.min = minYearEnergy;
        yearSlider.max = maxYearEnergy;
        yearSlider.value = maxYearEnergy;
        minYearDisplay.textContent = minYearEnergy;
        maxYearDisplay.textContent = maxYearEnergy;
    } else if (map.hasLayer(co2EmissionsPerCapitaLayer)) {
        yearSlider.min = minYearCo2Pc;
        yearSlider.max = maxYearCo2Pc;
        yearSlider.value = maxYearCo2Pc;
        minYearDisplay.textContent = minYearCo2Pc;
        maxYearDisplay.textContent = maxYearCo2Pc;
    }
}

yearSlider.addEventListener('input', function(e) {
    selectedYear = parseInt(e.target.value);

    document.getElementById('yearDisplay').textContent = selectedYear;
});

$.ajax({
    url: 'http://10.152.57.134:8080/geoserver/heidemann/ows',
    data: {
        version: '1.0.0',
        request: 'GetFeature',
        service: 'WFS',
        typeName: 'heidemann:world_energy_share',
        maxFeatures: '2000',
        outputFormat: 'application/json'
    },
    dataType: 'json',
    jsonpCallback: 'getJson',
    success: handleEnergyJson
    });

function handleEnergyJson(data) {
    energyLayer.addData(data);
    for (var feature of data.features) {
        minyear_c = feature.properties.minyear_c;
        if (minyear_c !== null &&  minyear_c < minYearEnergy) {
            minYearEnergy = minyear_c;
        }
        if (feature.properties.maxyear_c > maxYearEnergy) {
            maxYearEnergy = feature.properties.maxyear_c;
        }
    }
    document.getElementById('minYearEnergy').textContent = minYearEnergy;
    document.getElementById('maxYearEnergy').textContent = maxYearEnergy;
    document.getElementById('yearSlider').min = minYearEnergy;
    document.getElementById('yearSlider').max = maxYearEnergy;
    document.getElementById('yearSlider').value = maxYearEnergy;
}

function onEachFeature(feature, layer) {
    var name = feature.properties.name_de;

    var year_c = feature.properties.year_c ? JSON.parse(feature.properties.year_c) : null;
    var year_g = feature.properties.year_g ? JSON.parse(feature.properties.year_g) : null;
    var year_o = feature.properties.year_o ? JSON.parse(feature.properties.year_o) : null;
    var year_n = feature.properties.year_n ? JSON.parse(feature.properties.year_n) : null;
    var year_h = feature.properties.year_h ? JSON.parse(feature.properties.year_h) : null;
    var year_s = feature.properties.year_s ? JSON.parse(feature.properties.year_s) : null;
    var year_w = feature.properties.year_w ? JSON.parse(feature.properties.year_w) : null;

    var coal = feature.properties.coal ? JSON.parse(feature.properties.coal) : null;
    var gas = feature.properties.gas ? JSON.parse(feature.properties.gas) : null;
    var oil = feature.properties.oil ? JSON.parse(feature.properties.oil) : null;
    var nuclear = feature.properties.nuclear ? JSON.parse(feature.properties.nuclear) : null;
    var hydro = feature.properties.hydro ? JSON.parse(feature.properties.hydro) : null;
    var solar = feature.properties.solar ? JSON.parse(feature.properties.solar) : null;
    var wind = feature.properties.wind ? JSON.parse(feature.properties.wind) : null;

    var coalIndex = year_c ? year_c.indexOf(selectedYear) : -1;
    var gasIndex = year_g ? year_g.indexOf(selectedYear) : -1;
    var oilIndex = year_o ? year_o.indexOf(selectedYear) : -1;
    var nuclearIndex = year_n ? year_n.indexOf(selectedYear) : -1;
    var hydroIndex = year_h ? year_h.indexOf(selectedYear) : -1;
    var solarIndex = year_s ? year_s.indexOf(selectedYear) : -1;
    var windIndex = year_w ? year_w.indexOf(selectedYear) : -1;

    var data = [];
    var labels = [];
    
    var bgColors = [];

    if (coalIndex >= 0 && coal[coalIndex]) {
        data.push(coal[coalIndex]);
        labels.push('Kohle');
        bgColors.push(colors['Kohle']);
    }
    if (gasIndex >= 0 && gas[gasIndex]) {
        data.push(gas[gasIndex]);
        labels.push('Gas');
        bgColors.push(colors['Gas']);
    }
    if (oilIndex >= 0 && oil[oilIndex]) {
        data.push(oil[oilIndex]);
        labels.push('Öl');
        bgColors.push(colors['Öl']);
    }
    if (nuclearIndex >= 0 && nuclear[nuclearIndex]) {
        data.push(nuclear[nuclearIndex]);
        labels.push('Nuklear');
        bgColors.push(colors['Nuklear']);
    }
    if (hydroIndex >= 0 && hydro[hydroIndex]) {
        data.push(hydro[hydroIndex]);
        labels.push('Hydro');
        bgColors.push(colors['Hydro']);
    }
    if (solarIndex >= 0 && solar[solarIndex]) {
        data.push(solar[solarIndex]);
        labels.push('Solar');
        bgColors.push(colors['Solar']);
    }
    if (windIndex >= 0 && wind[windIndex]) {
        data.push(wind[windIndex]);
        labels.push('Wind');
        bgColors.push(colors['Wind']);
    }

    if (data.length > 0) {    
        var pieOptions = {
            type: 'pie',
            width: 40, height: 40,
            data: data.map(val => parseFloat(val.toFixed(2))),
            colors: bgColors
        };

        var center = layer.getBounds().getCenter();
        var pieChart = L.minichart(center, pieOptions);
        map.addLayer(pieChart); 
    }

    var popupContent = `
        <strong>${name}</strong><br/>
        ${labels.map((label, index) => `${label}: ${parseFloat(data[index].toFixed(2))}%`).join('<br/>')}
    `;
    layer.bindPopup(popupContent);
}


map.addLayer(energyLayer);

var baseMaps = {
    "Google Satellit": googleSat,
    "Google Streets": googleStreets,
    "OSM Map": osmMapnik,     
};

var overlayMaps = {
    "Energiemix": energyLayer, 
};

L.control.layers(baseMaps, overlayMaps).addTo(map);

var legend = L.control({position: 'bottomright'});

legend.onAdd = function (map) {
    var div = L.DomUtil.create('div', 'info legend');
    labels.forEach((label) => {
        div.innerHTML += 
        '<i style="background:' + colors[label] + '"></i> ' +
        (label ? label + '<br>' : '+');
    });

    return div;
};

legend.addTo(map);