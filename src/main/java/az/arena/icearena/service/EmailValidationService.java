package az.arena.icearena.service;

import az.arena.icearena.exception.BadRequestException;
import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailValidationService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public void validate(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim();
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("E-poçt ünvanı düzgün formatda deyil (məs: ad@gmail.com).");
        }

        int atIdx = email.lastIndexOf('@');
        String domain = email.substring(atIdx + 1).toLowerCase(Locale.ROOT);

        if (!hasValidMailDomain(domain)) {
            throw new BadRequestException("Bu e-poçt domeni mövcud deyil və ya poçt qəbul etmir.");
        }
    }

    private boolean hasValidMailDomain(String domain) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "2500");
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            Attribute mx = attrs != null ? attrs.get("MX") : null;
            return mx != null && mx.size() > 0;
        } catch (NameNotFoundException ex) {
            return false;
        } catch (CommunicationException ex) {
            log.warn("DNS lookup failed for email domain '{}': {}", domain, ex.getMessage());
            return true;
        } catch (NamingException ex) {
            return false;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignored) {
                    // ignore
                }
            }
        }
    }
}
