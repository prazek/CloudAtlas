$(function() {
    function buildAllZones(data) {
        var zones = $("<div />");
        for (var zoneName in data) {
            zones.append($("<h2 />").text(zoneName));
            zones.append(buildAttributesTable(zoneName, data[zoneName]));
        }
        return zones;
    }

    function update() {
        $.post("/attributes/", function(data) {
            var table = buildAllZones(data);
            $("#zones").html(table);
            $("#attributes").html(table);
        }).fail(function(xhr, status, error) {
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    }

    //var myInterval = setInterval(update, 1000);

    update();

    $("#install-query-form").submit(function(e) {
        e.preventDefault();
        var name = $("#queryName").val();
        var code = $("#queryCode").val();
        $.post("/installQuery/", {"queryName": name, "query": code}, function(data) {
            location.reload();
        }).fail(function(xhr, status, error) {
            if (xhr.status == 400) {
                $("#maybeErrorInstallQuery").text(xhr.responseText);
            }
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    });

    function updateInstalledQueries() {
        $.get("/installedQueries/", function(data) {
            var queries = $("<table />");
            for (var q in data.values) {
                var row = $("<tr />");
                row.append($("<td />").text(q));
                row.append($("<td />").text(attributeValueToString(data.values[q])));
                var removeButton = $("<button class=\"remove-query-button\" type=\"button\">Remove</button>")
                    .data("queryName", q)
                    .click(function(e) {
                        var queryName = $(e.currentTarget).data("queryName");
                        $.post("/uninstallQuery/", {"queryName": queryName}, function(data) {
                            location.reload();
                        }).fail(function(xhr, status, error) {
                            console.log(xhr);
                            console.log(status);
                            console.log(error);
                        });
                    });
                row.append($("<td />").html(removeButton));
                queries.append(row);
            }
            $("#installed-query-list").html(queries);
        }).fail(function(xhr, status, error) {
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    }

    updateInstalledQueries();

    var contacts = []

    function contactsFromData(data) {
        return JSON.parse(data).map(function(contact) { return {name: contact.name, address: contact.address}; });
    }

    function loadFallbackContacts() {
        var newContacts = [];
        $(".contact-input-set").each(function(i, contactData) {
            var contact = {}
            $(contactData).children('input').each(function(i, d) {
                // FRAGILE
                if (i == 0) {
                    contact["name"] = d.value;
                } else if (i == 1) {
                    contact["address"] = d.value;
                }
            })
            newContacts.push(contact);
        });
        contacts = newContacts;
    }

    function removeContact(e) {
        e.preventDefault();
        $(e.currentTarget.parentNode).remove();
    }

    function renderFallbackContacts() {
        var d = $("#fallback-contacts-inputs");
        d.html("");
        for (var i = 0; i < contacts.length; ++i) {
            var contact = $("<div class=\"contact-input-set\">");
            contact.append($("<input class=\"contact-name\">").val(contacts[i].name));
            contact.append($("<input class=\"contact-address\">").val(contacts[i].address));
            contact.append($("<button class=\"remove-contact\">-</button>").click(removeContact));
            d.append(contact);
        }
    }

    function updateFallbackContacts() {
        $.get("/fallbackContacts/", function(data) {
            var queries = $("<table />");
            contacts = contactsFromData(data);
            renderFallbackContacts();
        }).fail(function(xhr, status, error) {
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    }

    updateFallbackContacts();

    $("#new-fallback-contact").click(function(e) {
        e.preventDefault();
        contacts.push({"name": "", "address": ""});
        renderFallbackContacts();
    });

    $("#fallback-contacts").submit(function(e) {
        e.preventDefault();
        loadFallbackContacts();
        $.post("/fallbackContacts/", JSON.stringify(contacts), function(data) {
            $("#maybeErrorFallbackContacts").text("");
        }).fail(function(xhr, status, error) {
            if (xhr.status == 400) {
                $("#maybeErrorFallbackContacts").text(xhr.responseText);
            }
            console.log(xhr);
            console.log(status);
            console.log(error);
        });
    })
});
