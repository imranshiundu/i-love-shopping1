package com.iloveshopping.validation;

import com.iloveshopping.dto.order.CheckoutRequest.AddressRequest;
import com.iloveshopping.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates shipping/billing addresses for plausibility. Catches gibberish input
 * (random keystrokes) while still allowing real addresses without requiring a
 * third-party geocoding API.
 */
@Component
public class AddressValidator {

    private static final Set<String> KENYA_COUNTIES = Arrays.stream(new String[]{
            "Mombasa", "Kwale", "Kilifi", "Tana River", "Lamu", "Taita-Taveta", "Garissa",
            "Wajir", "Mandera", "Marsabit", "Isiolo", "Meru", "Tharaka-Nithi", "Embu",
            "Kitui", "Machakos", "Makueni", "Nyandarua", "Nyeri", "Kirinyaga", "Murang'a",
            "Kiambu", "Turkana", "West Pokot", "Samburu", "Trans Nzoia", "Uasin Gishu",
            "Elgeyo-Marakwet", "Nandi", "Baringo", "Laikipia", "Nakuru", "Narok", "Kajiado",
            "Kericho", "Bomet", "Kakamega", "Vihiga", "Bungoma", "Busia", "Siaya", "Kisumu",
            "Homa Bay", "Migori", "Kisii", "Nyamira", "Nairobi"
    }).map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

    private static final Set<String> KENYA_CITIES = Arrays.stream(new String[]{
            "Nairobi", "Mombasa", "Kisumu", "Nakuru", "Eldoret", "Thika", "Naivasha",
            "Kikuyu", "Ruiru", "Juja", "Limuru", "Kitengela", "Athi River", "Ongata Rongai",
            "Kajiado", "Machakos", "Nyeri", "Embu", "Meru", "Garissa", "Kakamega",
            "Kisii", "Kericho", "Bungoma", "Busia", "Vihiga", "Nanyuki", "Nyeri"
    }).map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

    private static final Set<String> TOWNS = Arrays.stream(new String[]{
            "westlands", "kilimani", "karen", "langata", "kasarani", "embakasi", "ruaraka",
            "dagoretti", "makadara", "kamukunji", "starehe", "mukuru", "south b", "south c",
            "buruburu", "donholm", "kayole", "umoj a", "juja", "karen", "lavington",
            "hurlingham", "parklands", "kilimani", "upper hill", "ngong road", "loresho"
    }).map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

    public void validate(AddressRequest address, String label) {
        if (address == null) {
            throw ApiException.badRequest(label + " address is required");
        }

        // Basic plausibility: at least one letter in name
        String name = address.getName() == null ? "" : address.getName().trim();
        if (name.length() < 2 || !name.chars().anyMatch(Character::isLetter)) {
            throw ApiException.badRequest(label + ": please enter a valid full name");
        }
        // Name must not be random keyboard gibberish (rejects "asdfgh", "qwerty", "zxcvb").
        if (isGibberish(name)) {
            throw ApiException.badRequest(label + ": please enter a valid full name (e.g. \"Amina Wanjiru\")");
        }
        // Names must contain at least one vowel in a real word position
        String nameAlpha = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (nameAlpha.length() >= 4 && !hasVowel(nameAlpha)) {
            throw ApiException.badRequest(label + ": please enter a valid full name (e.g. \"Amina Wanjiru\")");
        }

        // Street address: must contain a letter and a digit (number), min length
        String line1 = address.getLine1() == null ? "" : address.getLine1().trim();
        if (line1.length() < 5
                || !line1.chars().anyMatch(Character::isLetter)
                || !line1.chars().anyMatch(Character::isDigit)) {
            throw ApiException.badRequest(
                    label + ": street address must include a street name and a building/house number (e.g. \"123 Kenyatta Avenue\")");
        }
        if (isGibberish(line1)) {
            throw ApiException.badRequest(label + ": street address does not look valid (e.g. \"123 Kenyatta Avenue\")");
        }

        // City must be a letter string, min length, not gibberish
        String city = address.getCity() == null ? "" : address.getCity().trim();
        if (city.length() < 3 || !city.chars().anyMatch(Character::isLetter) || isGibberish(city)) {
            throw ApiException.badRequest(label + ": please enter a valid city/town (e.g. \"Nairobi\")");
        }

        // County/state must match a known Kenyan county when country is Kenya
        String state = address.getState() == null ? "" : address.getState().trim();
        String country = address.getCountry() == null ? "" : address.getCountry().trim().toUpperCase(Locale.ROOT);
        if (state.length() < 2 || isGibberish(state)) {
            throw ApiException.badRequest(label + ": please enter a valid county/state (e.g. \"Nairobi\")");
        }
        if (country.equals("KE") || country.equals("KENYA")) {
            String stateLower = state.toLowerCase(Locale.ROOT);
            boolean countyKnown = KENYA_COUNTIES.contains(stateLower)
                    || KENYA_COUNTIES.contains(stateLower.replace("-", " "))
                    || stateLower.equals("nairobi");
            if (!countyKnown) {
                throw ApiException.badRequest(
                        label + ": \"" + state + "\" is not a recognized Kenyan county. Examples: Nairobi, Mombasa, Nakuru, Kiambu");
            }
        }

        // Postal code: numeric, 3-6 digits (Kenya uses 5-digit codes e.g. 00100)
        String postal = address.getPostalCode() == null ? "" : address.getPostalCode().trim();
        if (!postal.matches("\\d{3,6}")) {
            throw ApiException.badRequest(label + ": postal code must be 3–6 digits (e.g. 00100)");
        }

        // Phone: digits only, 9–13 digits when provided
        String phone = address.getPhone() == null ? "" : address.getPhone().replaceAll("[^0-9]", "");
        if (!phone.isEmpty() && (phone.length() < 9 || phone.length() > 13)) {
            throw ApiException.badRequest(label + ": enter a valid phone number (e.g. 0712345678)");
        }
    }

    private boolean hasVowel(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        return lower.chars().anyMatch(c -> "aeiou".indexOf(c) >= 0);
    }

    /**
     * Heuristic for random keyboard mash (e.g. "asdfgh", "qwerty", "zzzzz", "xcvbnm").
     */
    private boolean isGibberish(String s) {
        String lower = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (lower.length() < 2) return true;
        // Long run of a single repeated character
        long distinct = lower.chars().distinct().count();
        if (lower.length() >= 4 && distinct == 1) return true;
        // All consonants, no vowel, and length >= 5 -> unlikely to be a word
        if (lower.length() >= 5 && !hasVowel(lower)) return true;
        // Classic keyboard rows (exact mash patterns, single token only)
        if (!s.trim().contains(" ") && lower.matches("(asdfgh|qwerty|zxcvbn|sdfg|wert|dfgh|fghj|hjkl|qwe|asd|zxc)+")) return true;
        // Consonant-to-vowel ratio too high (e.g. "strghmn") -> keyboard mash
        if (lower.length() >= 6) {
            long consonants = lower.chars().filter(c -> "aeiou".indexOf(c) < 0).count();
            if (consonants * 1.0 / lower.length() > 0.85) return true;
        }
        return false;
    }
}
