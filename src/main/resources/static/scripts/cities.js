/* global window, M */
"use strict";

var CityLocator = (function ($, window) {
	var DATA_URL = (window.CONTEXT_PATH || "") + "/cities.json";
	var PREFIX_LENGTH = 3;
	var MIN_SUGGEST_CHARS = 2;
	var datasetCache = null;
	var datasetPromise = null;
	var accentRegex = /[\u0300-\u036f]/g;
	var manualAliases = Object.create(null);

	function addAlias(code, aliases) {
		for (var i = 0; i < aliases.length; i++) {
			var normalized = normalizeText(aliases[i]);
			if (normalized) {
				manualAliases[normalized] = code;
			}
		}
	}

	addAlias("US", ["usa", "united states", "u.s.a", "america", "united states of america", "u.s."]);
	addAlias("GB", ["united kingdom", "uk", "england", "great britain", "scotland", "wales", "britain"]);
	addAlias("AE", ["united arab emirates", "uae"]);
	addAlias("CZ", ["czechia", "czech republic"]);
	addAlias("CD", ["democratic republic of the congo", "drc"]);
	addAlias("KP", ["north korea"]);
	addAlias("KR", ["south korea", "republic of korea"]);
	addAlias("RU", ["russia", "russian federation"]);
	addAlias("IR", ["iran", "islamic republic of iran"]);
	addAlias("SY", ["syria", "syrian arab republic"]);
	addAlias("LA", ["laos", "lao people's democratic republic"]);

	function normalizeText(value) {
		if (value === null || value === undefined) {
			return "";
		}
		var text = value.toString();
		if (text.normalize) {
			text = text.normalize("NFKD");
		}
		text = text.replace(accentRegex, "");
		text = text.replace(/\s+/g, " ");
		return text.trim().toLowerCase();
	}

	function ensureDataset() {
		if (datasetCache) {
			return $.Deferred().resolve(datasetCache).promise();
		}
		if (!datasetPromise) {
			var deferred = $.Deferred();
			$.getJSON(DATA_URL).done(function (data) {
				var cities = Array.isArray(data) ? data : [];
				datasetCache = buildIndexes(cities);
				deferred.resolve(datasetCache);
			}).fail(function (_jqxhr, textStatus, errorThrown) {
				datasetPromise = null;
				var message = errorThrown || textStatus || "Failed to load city dataset.";
				deferred.reject(new Error(message));
			});
			datasetPromise = deferred.promise();
		}
		return datasetPromise;
	}

	function buildIndexes(rawCities) {
		var keyed = Object.create(null);
		var byName = Object.create(null);
		var prefixIndex = Object.create(null);
		var isoAliases = Object.create(null);
		var countryNames = Object.create(null);
		var regionFormatter = createRegionFormatter();

		for (var i = 0; i < rawCities.length; i++) {
			var city = rawCities[i];
			var name = city && city.name ? city.name : "";
			var country = city && city.country ? city.country.toString().toUpperCase() : "";
			var lat = parseFloat(city && city.lat);
			var lng = parseFloat(city && city.lng);
			if (!name || !country || isNaN(lat) || isNaN(lng)) {
				continue;
			}
			var normalizedName = normalizeText(name);
			if (!normalizedName) {
				continue;
			}
			var record = {
				name: name,
				country: country,
				lat: lat,
				lng: lng,
				normalizedName: normalizedName
			};
			keyed[normalizedName + "|" + country] = record;
			if (!byName[normalizedName]) {
				byName[normalizedName] = [];
			}
			byName[normalizedName].push(record);
			var prefix = normalizedName.slice(0, PREFIX_LENGTH);
			if (prefix) {
				if (!prefixIndex[prefix]) {
					prefixIndex[prefix] = [];
				}
				prefixIndex[prefix].push(record);
			}
			if (!countryNames[country]) {
				var readable = country;
				if (regionFormatter) {
					try {
						readable = regionFormatter.of(country) || country;
					} catch (err) {
						readable = country;
					}
				}
				countryNames[country] = readable;
				isoAliases[normalizeText(readable)] = country;
				isoAliases[country.toLowerCase()] = country;
			}
		}

		Object.keys(manualAliases).forEach(function (alias) {
			if (!isoAliases[alias]) {
				isoAliases[alias] = manualAliases[alias];
			}
		});

		return {
			keyed: keyed,
			byName: byName,
			prefixIndex: prefixIndex,
			isoAliases: isoAliases,
			countryNames: countryNames
		};
	}

	function parseLocationText(text) {
		if (!text) {
			return {city: "", country: ""};
		}
		var cleaned = text.trim();
		if (!cleaned) {
			return {city: "", country: ""};
		}
		var codeMatch = cleaned.match(/\(([^)]+)\)\s*$/);
		var explicit = codeMatch ? codeMatch[1] : "";
		if (explicit) {
			cleaned = cleaned.replace(/\(([^)]+)\)\s*$/, "").trim();
		}
		var parts = cleaned.split(",");
		var city = parts.shift() || "";
		var country = parts.length ? parts.join(",") : "";
		return {
			city: city.trim(),
			country: (country || explicit || "").trim()
		};
	}

	function resolveCountryCode(value, dataset) {
		if (!value) {
			return "";
		}
		var trimmed = value.trim();
		if (/^[a-z]{2}$/i.test(trimmed)) {
			return trimmed.toUpperCase();
		}
		var normalized = normalizeText(trimmed);
		return dataset.isoAliases[normalized] || "";
	}

	function formatResult(record, dataset) {
		var countryName = dataset.countryNames[record.country] || record.country;
		return {
			name: record.name,
			lat: record.lat,
			lng: record.lng,
			countryCode: record.country,
			countryName: countryName,
			label: record.name + ", " + countryName + " (" + record.country + ")"
		};
	}

	function findExactMatch(text, dataset) {
		var parsed = parseLocationText(text);
		var normalizedCity = normalizeText(parsed.city);
		if (!normalizedCity) {
			return null;
		}
		var countryCode = resolveCountryCode(parsed.country, dataset);
		if (countryCode) {
			var strictKey = normalizedCity + "|" + countryCode;
			if (dataset.keyed[strictKey]) {
				return formatResult(dataset.keyed[strictKey], dataset);
			}
		}
		var matches = dataset.byName[normalizedCity];
		if (!matches || !matches.length) {
			return null;
		}
		if (matches.length === 1) {
			return formatResult(matches[0], dataset);
		}
		if (countryCode) {
			for (var i = 0; i < matches.length; i++) {
				if (matches[i].country === countryCode) {
					return formatResult(matches[i], dataset);
				}
			}
		}
		return formatResult(matches[0], dataset);
	}

	function findSuggestions(text, limit, dataset) {
		var parsed = parseLocationText(text);
		var normalized = normalizeText(parsed.city);
		if (!normalized || normalized.length < MIN_SUGGEST_CHARS) {
			return [];
		}
		var countryCode = resolveCountryCode(parsed.country, dataset);
		var prefix = normalized.slice(0, PREFIX_LENGTH);
		var bucket = dataset.prefixIndex[prefix] || [];
		var seen = Object.create(null);
		var maxItems = Math.max(1, limit || 8);
		var results = [];

		for (var i = 0; i < bucket.length && results.length < maxItems; i++) {
			var candidate = bucket[i];
			if (candidate.normalizedName.indexOf(normalized) !== 0) {
				continue;
			}
			if (countryCode && candidate.country !== countryCode) {
				continue;
			}
			var key = candidate.name + "|" + candidate.country;
			if (seen[key]) {
				continue;
			}
			seen[key] = true;
			results.push(formatResult(candidate, dataset));
		}

		if (!results.length) {
			var sameName = dataset.byName[normalized] || [];
			for (var j = 0; j < sameName.length && results.length < maxItems; j++) {
				var fallbackKey = sameName[j].name + "|" + sameName[j].country;
				if (seen[fallbackKey]) {
					continue;
				}
				if (countryCode && sameName[j].country !== countryCode) {
					continue;
				}
				seen[fallbackKey] = true;
				results.push(formatResult(sameName[j], dataset));
			}
		}

		return results;
	}

	function createRegionFormatter() {
		if (window.Intl && typeof window.Intl.DisplayNames === "function") {
			try {
				return new window.Intl.DisplayNames(["en"], {type: "region"});
			} catch (err) {
				return null;
			}
		}
		return null;
	}

	return {
		ensureDataset: ensureDataset,
		lookup: function (text) {
			return ensureDataset().then(function (dataset) {
				return findExactMatch(text, dataset);
			});
		},
		suggest: function (text, limit) {
			return ensureDataset().then(function (dataset) {
				return findSuggestions(text, limit, dataset);
			});
		}
	};
}(jQuery, window));

(function ($, window) {
	$(document).on("focus", "input.locationbox", function () {
		attachCityLocator($(this));
	});

	function attachCityLocator($input) {
		if (!$input || !$input.length || $input.data("cityLocatorBound")) {
			return;
		}
		$input.data("cityLocatorBound", true);
		$input.attr("autocomplete", "off");
		var listOfCitiesFound = {};
		var listOfCitiesFoundData = {};

		$input.autocomplete({
			data: {},
			sortFunction: false,
			limit: 10,
			onAutocomplete: function (el) {
				if (listOfCitiesFound[el]) {
					updateLatLngFields($input, listOfCitiesFound[el]);
				}
			}
		});

		$input.keyup(function (e) {
			// Don't capture enter or arrow key usage.
			if (e.which === 13 || e.which === 38 || e.which === 40) {
				return;
			}
			var instance = M.Autocomplete.getInstance(this);
			var value = $input.val();
			if (!value || value.length < 2) {
				return;
			}
			CityLocator.suggest(value, 10).done(function (data) {
				for (var i = 0; i < data.length; i++) {
					listOfCitiesFoundData[data[i].label] = null;
					listOfCitiesFound[data[i].label] = data[i];
				}
				instance.updateData(listOfCitiesFoundData);
				instance.open();

			});
		});

	}

	function updateLatLngFields($input, match) {
		var latlngField = $input.closest("form").find("input[name=latlng], input.latlngbox").first();
		if (latlngField.length) {
			latlngField.val(match ? match.lat + "," + match.lng : "");
		}
	}
}(jQuery, window));
