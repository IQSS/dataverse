package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.util.SystemConfig;
import org.passay.*;
import org.passay.dictionary.WordListDictionary;
import org.passay.dictionary.WordLists;
import org.passay.dictionary.sort.ArraysSort;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * PasswordValidatorServiceBean
 * <p>
 * The purpose of this class is to validate passwords according to a set of rules as described in:
 * https://github.com/IQSS/dataverse/issues/3150
 * These contemporary rules govern the way passwords and accounts are protected in order to keep up with current level 3
 * sensitivity data standards.
 * <p>
 * This class will offer presets:
 * Rule 1. It will use a dictionary to block the use of commonly used passwords.
 * <p>
 * Rule 2. It will include at least one character from at least three out of of these four categories:
 * Uppercase letter
 * Lowercase letter
 * Digit
 * Special character ( a whitespace is not a character )
 * <p>
 * Rule 3. It will allow either:
 * a. 8 password length minimum with an annual password expiration
 * b. 10 password length minimum
 * Rule 4. It will forgo all the above three requirements for passwords that have a minimum length of 20.
 * <p>
 * All presets can be tweaked by applying new settings via the admin API of VM arguments.
 * When set VM arguments always overrule admin API settings.
 * <p>
 * Two validator types implement the rulesets.
 * GoodStrengthValidator: applies rule 4 for passwords with a length equal or greater than MIN_LENGTH_BIG_LENGTH
 * StandardValidator: applies rules 1, 2 and 3 for passwords with a length less than MIN_LENGTH_BIG_LENGTH
 * <p>
 * For more information on the library used here, @see http://passay.org
 *
 * @author Lucien van Wouw <lwo@iisg.nl>
 */
@Named
@Stateless
public class PasswordValidatorServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(PasswordValidatorServiceBean.class.getCanonicalName());
    private static String DICTIONARY_FILES = "weak_passwords.txt";

    private enum ValidatorTypes {
        GoodStrengthValidator, StandardValidator
    }

    @SuppressWarnings("unchecked")
    private final static LinkedHashMap<ValidatorTypes, PasswordValidator> validators = new LinkedHashMap(2);
    private int expirationDays;
    private int expirationMaxLength;
    private int goodStrength;
    private int maxLength;
    private int minLength;
    private int numberOfCharacteristics;
    private String dictionaries = DICTIONARY_FILES;
    private PropertiesMessageResolver messageResolver;

    @EJB
    SystemConfig systemConfig;

    PasswordValidatorServiceBean() {
        final Properties properties = PropertiesMessageResolver.getDefaultProperties();
        properties.setProperty(GoodStrengthRule.ERROR_CODE_GOODSTRENGTH, GoodStrengthRule.ERROR_MESSAGE_GOODSTRENGTH);
        properties.setProperty(ExpirationRule.ERROR_CODE_EXPIRED, ExpirationRule.ERROR_MESSAGE_EXPIRED);
        messageResolver = new PropertiesMessageResolver(properties);
    }

    /**
     * validate
     *
     * @param password the password to check
     * @return A List with human readable error messages. Empty when the password is valid.
     */
    public List<String> validate(String password) {
        return validate(password, new Date());
    }

    /**
     * validate
     * <p>
     * Validates the password properties and determine if their valid.
     * Password reset consumers would use this method, because there should be no modification date check for new
     * passwords.
     *
     * @param password                 the password to check
     * @param passwordModificationTime The time the password was set or changed.
     * @return A List with human readable error messages. Empty when the password is valid.
     */
    public List<String> validate(String password, Date passwordModificationTime) {
        return validate(password, passwordModificationTime, true);
    }

    /**
     * validate
     * <p>
     * Validates the password properties and its modification date and determine if their valid.
     *
     * @param passwordModificationTime The time the password was set or changed.
     * @param password                 The password to check
     * @param isHumanReadable          The expression of the error messages. True if the audience is human.
     * @return A List with error messages. Empty when the password is valid.
     */
    public List<String> validate(String password, Date passwordModificationTime, boolean isHumanReadable) {

        init();
        final PasswordData passwordData = PasswordData.newInstance(password, String.valueOf(passwordModificationTime.getTime()), null);
        final RuleResult result = new RuleResult();
        for (PasswordValidator currentUser : validators.values()) {
            RuleResult r = currentUser.validate(passwordData);
            if (r.isValid())
                return Collections.emptyList();
            result.getDetails().addAll(r.getDetails());
        }

        if (isHumanReadable) {
            return validators.get(ValidatorTypes.StandardValidator).getMessages(result);
        } else {
            return result.getDetails().stream().map(RuleResultDetail::getErrorCode).collect(Collectors.toList());
        }
    }

    /**
     * init
     * <p>
     * Instantiates and caches the validators.
     */
    private void init() {
        addStandardValidator();
        addGoodStrengthValidator();
    }


    /**
     * goodStrengthValidator
     * <p>
     * Apply Rule 4: It will forgo all the above three requirements for passwords that have a minimum length of
     * MIN_LENGTH_BIG_LENGTH.
     */
    private void addGoodStrengthValidator() {

        int goodStrength = getGoodStrength();
        if (goodStrength != 0) {
            PasswordValidator passwordValidator = validators.get(ValidatorTypes.GoodStrengthValidator);
            if (passwordValidator == null) {
                final GoodStrengthRule lengthRule = new GoodStrengthRule();
                lengthRule.setMinimumLength(goodStrength);
                final List<Rule> rules = Collections.singletonList(lengthRule);
                passwordValidator = new PasswordValidator(messageResolver, rules);
                validators.put(ValidatorTypes.GoodStrengthValidator, passwordValidator);
            }
        }
    }


    /**
     * standardValidator
     * <p>
     * Apply Rules 1, 2 and 3.
     */
    private void addStandardValidator() {
        int maxLength = getMaxLength();
        int minLength = getMinLength();
        int numberOfCharacteristics = getNumberOfCharacteristics();
        PasswordValidator passwordValidator = validators.get(ValidatorTypes.StandardValidator);
        if (passwordValidator == null) {
            final List<Rule> rules = new ArrayList<>(4);
            rules.add(dictionaryRule());
            final LengthRule lengthRule = new LengthRule();
            if (maxLength != 0) {
                lengthRule.setMaximumLength(maxLength);
            }
            if (minLength != 0) {
                lengthRule.setMinimumLength(minLength);
            }
            rules.add(lengthRule);
            rules.add(new ExpirationRule(getExpirationMaxLength(), getExpirationDays()));
            if (numberOfCharacteristics != 0)
                rules.add(characterRule());
            passwordValidator = new PasswordValidator(messageResolver, rules);
            validators.put(ValidatorTypes.StandardValidator, passwordValidator);
        }
    }


    /**
     * dictionaryRule
     * <p>
     * Reads in the getDictionaries from a file.
     *
     * @return A rule.
     */
    private DictionaryRule dictionaryRule() {
        DictionaryRule rule = null;
        try {
            rule = new DictionaryRule(
                    new WordListDictionary(WordLists.createFromReader(
                            getDictionaries(),
                            false,
                            new ArraysSort())));
        } catch (IOException e) {
            logger.log(Level.CONFIG, e.getMessage());
        }
        return rule;
    }


    /**
     * getDictionaries
     *
     * @return A list of readers for each dictionary.
     */
    private FileReader[] getDictionaries() {

        setDictionaries(systemConfig == null ? this.dictionaries : systemConfig.getPVDictionaries());

        List<String> files = Arrays.asList(this.dictionaries.split(","));
        List<FileReader> fileReaders = new ArrayList<>(files.size());
        files.forEach(file -> {
            try {
                fileReaders.add(new FileReader(file));
            } catch (FileNotFoundException e) {
                logger.log(Level.CONFIG, e.getMessage());
            }
        });
        if (fileReaders.size() == 0)
            logger.warning("Dictionary was set, but none was read in.");
        return fileReaders.toArray(new FileReader[fileReaders.size()]);
    }

    void setDictionaries(String dictionaries) {
        if (dictionaries == null) {
            final URL url = PasswordValidatorServiceBean.class.getResource(DICTIONARY_FILES);
            if (url == null) {
                logger.warning("PwDictionaries not set and no default password file found: " + DICTIONARY_FILES);
                dictionaries = DICTIONARY_FILES;
            } else
                dictionaries = url.getPath() + File.pathSeparator + url.getFile();
        }
        if (!dictionaries.equals(this.dictionaries)) {
            this.dictionaries = dictionaries;
            validators.remove(ValidatorTypes.StandardValidator);
        }
    }


    /**
     * characterRule
     * <p>
     * Sets a this number of characteristics N from M rules.
     *
     * @return A CharacterCharacteristicsRule
     */
    private CharacterCharacteristicsRule characterRule() {
        final CharacterCharacteristicsRule characteristicsRule = new CharacterCharacteristicsRule();
        characteristicsRule.setNumberOfCharacteristics(getNumberOfCharacteristics());
        characteristicsRule.getRules().add(new CharacterRule(EnglishCharacterData.UpperCase, 1));
        characteristicsRule.getRules().add(new CharacterRule(EnglishCharacterData.LowerCase, 1));
        characteristicsRule.getRules().add(new CharacterRule(EnglishCharacterData.Digit, 1));
        characteristicsRule.getRules().add(new CharacterRule(EnglishCharacterData.Special, 1));
        return characteristicsRule;
    }

    /**
     * parseMessages
     *
     * @param messages A list of error messages
     * @return A Human readable string.
     */
    public static String parseMessages(List<String> messages) {
        return messages.stream()
                .map(Object::toString)
                .collect(Collectors.joining(" \n"));
    }

    /**
     * getGoodPasswordDescription
     * <p>
     * Describes all the characteristics of a valid password.
     */
    public String getGoodPasswordDescription() {
        final List<String> requirements = new ArrayList<>(4);
        if (getMinLength() != 0)
            requirements.add(String.format("a minimum of %s characters", getMinLength()));
        if (getMaxLength() != 0)
            requirements.add(String.format("a maximum of %s characters", getMaxLength()));
        if (getNumberOfCharacteristics() != 0)
            requirements.add(String.format("at least %s of these four characters: a number, special character, lowercase letter and uppercase letter", getNumberOfCharacteristics()));
        if (getGoodStrength() != 0)
            requirements.add(String.format("alternatively, the previous requirements do not apply for passwords of %s or more characters", getGoodStrength()));
        return requirements.stream()
                .map(Object::toString)
                .collect(Collectors.joining("; "));
    }


    /**
     * getGoodStrength
     * <p>
     * Get the length for the GoodStrengthValidator that determines what is a long, hard to brute force password.
     *
     * @return A length
     */
    private int getGoodStrength() {
        int goodStrength = systemConfig == null ? this.goodStrength : systemConfig.getPVGoodStrength();
        setGoodStrength(goodStrength);
        return this.goodStrength;
    }

    void setGoodStrength(int goodStrength) {
        if (goodStrength == 0)
            validators.remove(ValidatorTypes.GoodStrengthValidator);
        else {
            int minLength = getMinLength();
            if (goodStrength <= minLength) {
                int reset = minLength + 1;
                logger.log(Level.WARNING, "The PwGoodStrength " + goodStrength + " value competes with the" +
                        "PwMinLength value of " + minLength + " and is added  to " + reset);
                goodStrength = reset;
            }
        }
        if (this.goodStrength != goodStrength) {
            this.goodStrength = goodStrength;
            validators.remove(ValidatorTypes.GoodStrengthValidator);
        }
    }


    /**
     * getMaxLength
     * <p>
     * The maximum password length for the StandardValidator
     *
     * @return A length
     */
    private int getMaxLength() {
        int maxLength = systemConfig == null ? this.maxLength : systemConfig.getPVMaxLength();
        setMaxLength(maxLength);
        return this.maxLength;
    }

    void setMaxLength(int maxLength) {
        if (this.maxLength != maxLength) {
            this.maxLength = maxLength;
            validators.remove(ValidatorTypes.StandardValidator);
        }
    }


    /**
     * getMinLength
     * <p>
     * The minimum password length for the StandardValidator.
     *
     * @return A length
     */
    private int getMinLength() {
        int minLength = systemConfig == null ? this.minLength : systemConfig.getPVMinLength();
        setMinLength(minLength);
        return this.minLength;
    }

    void setMinLength(int minLength) {
        if (this.minLength != minLength) {
            this.minLength = minLength;
            validators.remove(ValidatorTypes.StandardValidator);
        }
    }


    /**
     * getExpirationDays
     * <p>
     * The number of days a passwords is good after its creation or modification date.
     * If set to zero, an expiration is not applied for the StandardValidator.
     *
     * @return A number
     */
    private int getExpirationDays() {
        int expirationDays = systemConfig == null ? this.expirationDays : systemConfig.getPVExpirationDays();
        setExpirationDays(expirationDays);
        return this.expirationDays;
    }

    void setExpirationDays(int expirationDays) {
        if (this.expirationDays != expirationDays) {
            this.expirationDays = expirationDays;
            validators.remove(ValidatorTypes.StandardValidator);
        }
    }

    void setNumberOfCharacteristics(int numberOfCharacteristics) {
        if (this.numberOfCharacteristics != numberOfCharacteristics) {
            this.numberOfCharacteristics = numberOfCharacteristics;
            validators.remove(ValidatorTypes.StandardValidator);
        }
    }

    private int getNumberOfCharacteristics() {
        int numberOfCharacteristics = systemConfig == null ? this.numberOfCharacteristics : systemConfig.getPVNumberOfCharacteristics();
        setNumberOfCharacteristics(numberOfCharacteristics);
        return this.numberOfCharacteristics;
    }


    /**
     * getExpirationMaxLength
     * <p>
     * The upper limit of a password length for which an expiration date will be applicable.
     *
     * @return A length
     */
    private int getExpirationMaxLength() {
        int expirationMaxLength = systemConfig == null ? this.expirationMaxLength : systemConfig.getPVExpirationMaxLength();
        setExpirationMaxLength(expirationMaxLength);
        return this.expirationMaxLength;
    }

    void setExpirationMaxLength(int expirationMaxLength) {
        if (this.expirationMaxLength != expirationMaxLength) {
            this.expirationMaxLength = expirationMaxLength;
            validators.remove(ValidatorTypes.StandardValidator);
        }
    }

}
