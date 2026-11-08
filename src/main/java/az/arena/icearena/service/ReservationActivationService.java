package az.arena.icearena.service;

import az.arena.icearena.entity.Reservation;
import java.util.List;

/**
 * Konki rezervasiyası taymerinin əl ilə aktivləşdirilməsi və
 * vaxtı bitmiş taymerlərin stokunun avtomatik qaytarılması.
 */
public interface ReservationActivationService {

    /** Bilet/vauçer kodu ilə qrupdakı bütün rezervasiyaları aktivləşdirir. */
    List<Reservation> activateByCode(String ticketCode, String username, boolean admin);

    /** Tək rezervasiya ID-si ilə (köhnə API uyğunluğu) aktivləşdirir. */
    List<Reservation> activateReservation(Long reservationId, String username, boolean admin);

    /** Vaxtı bitmiş aktiv konki rezervasiyalarının stokunu geri qaytarır. */
    void expireStaleReservations();
}
