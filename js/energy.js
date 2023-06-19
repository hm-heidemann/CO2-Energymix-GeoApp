var minYearEnergy = Number.MAX_SAFE_INTEGER;
var maxYearEnergy = 0;

var maxCo2 = 0;

let selectedYear = maxYearEnergy;

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
    var coalIndex = feature.properties.year_c ? feature.properties.year_c.indexOf(selectedYear) : -1;
    var gasIndex = feature.properties.year_g ? feature.properties.year_g.indexOf(selectedYear) : -1;
    var oilIndex = feature.properties.year_o ? feature.properties.year_o.indexOf(selectedYear) : -1;
    var nuclearIndex = feature.properties.year_n ? feature.properties.year_n.indexOf(selectedYear) : -1;
    var hydroIndex = feature.properties.year_h ? feature.properties.year_h.indexOf(selectedYear) : -1;
    var solarIndex = feature.properties.year_s ? feature.properties.year_s.indexOf(selectedYear) : -1;
    var windIndex = feature.properties.year_w ? feature.properties.year_w.indexOf(selectedYear) : -1;

    if (feature.properties && feature.properties.coal && coalIndex >= 0 && gasIndex >= 0 && oilIndex >= 0 && nuclearIndex >= 0 && hydroIndex >= 0 && solarIndex >= 0 && windIndex >= 0) {    
        var pieOptions = {
            type: 'pie',
            width: 40, height: 40,
            data: [
                feature.properties.coal[coalIndex], 
                feature.properties.gas[gasIndex], 
                feature.properties.oil[oilIndex], 
                feature.properties.nuclear[nuclearIndex], 
                feature.properties.hydro[hydroIndex], 
                feature.properties.solar[solarIndex], 
                feature.properties.wind[windIndex]
            ],
            labels: ['coal', 'gas', 'oil', 'nuclear', 'hydro', 'solar', 'wind'],
            backgroundColor: ['#000000', '#808080', '#FFD700', '#800000', '#0000FF', '#FFA500', '#008000'],
            hoverBackgroundColor: ['#000000', '#808080', '#FFD700', '#800000', '#0000FF', '#FFA500', '#008000']
        };

        var center = layer.getBounds().getCenter();
        var pieChart = L.minichart(center, pieOptions);
        map.addLayer(pieChart); 
    }
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
    return div;
};

legend.addTo(map);