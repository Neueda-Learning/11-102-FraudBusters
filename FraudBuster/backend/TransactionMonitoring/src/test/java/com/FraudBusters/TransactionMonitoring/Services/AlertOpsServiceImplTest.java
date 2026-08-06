package com.FraudBusters.TransactionMonitoring.Services;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.Impl.AlertOpsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertOpsServiceImplTest {

    @Mock
    private AlertEntityRepository alertEntityRepository;

    @InjectMocks
    private AlertOpsServiceImpl service;

    @Test
    void getAllAlerts_whenRepositoryReturnsRecords_thenReturnsSameList() {
        // given
        AlertEntity alert1 = alert(1L, "AL-1001");
        AlertEntity alert2 = alert(2L, "AL-1002");
        List<AlertEntity> expected = List.of(alert1, alert2);
        when(alertEntityRepository.findAll()).thenReturn(expected);

        // when
        List<AlertEntity> actual = service.getAllAlerts();

        // then
        assertSame(expected, actual);
        assertEquals(2, actual.size());
        verify(alertEntityRepository).findAll();
        verifyNoMoreInteractions(alertEntityRepository);
    }

    @Test
    void getAllAlerts_whenRepositoryReturnsEmpty_thenReturnsEmptyList() {
        // given
        when(alertEntityRepository.findAll()).thenReturn(List.of());

        // when
        List<AlertEntity> actual = service.getAllAlerts();

        // then
        assertEquals(0, actual.size());
        verify(alertEntityRepository).findAll();
        verifyNoMoreInteractions(alertEntityRepository);
    }

    @Test
    void getAllAlerts_whenRepositoryThrowsRuntimeException_thenPropagatesException() {
        // given
        RuntimeException repositoryError = new RuntimeException("DB unavailable");
        when(alertEntityRepository.findAll()).thenThrow(repositoryError);

        // when
        RuntimeException actual = assertThrows(RuntimeException.class, () -> service.getAllAlerts());

        // then
        assertSame(repositoryError, actual);
        verify(alertEntityRepository).findAll();
        verifyNoMoreInteractions(alertEntityRepository);
    }

    @Test
    void getActiveAlerts_whenRepositoryReturnsRecords_thenReturnsSameList() {
        // given
        AlertEntity active1 = alert(10L, "AL-2001");
        AlertEntity active2 = alert(11L, "AL-2002");
        List<AlertEntity> expected = List.of(active1, active2);
        when(alertEntityRepository.findAlertsWhoseAlertStatusIsNotDismissedAndClosed()).thenReturn(expected);

        // when
        List<AlertEntity> actual = service.getActiveAlerts();

        // then
        assertSame(expected, actual);
        assertEquals(2, actual.size());
        verify(alertEntityRepository).findAlertsWhoseAlertStatusIsNotDismissedAndClosed();
        verifyNoMoreInteractions(alertEntityRepository);
    }

    @Test
    void getActiveAlerts_whenRepositoryReturnsEmpty_thenReturnsEmptyList() {
        // given
        when(alertEntityRepository.findAlertsWhoseAlertStatusIsNotDismissedAndClosed()).thenReturn(List.of());

        // when
        List<AlertEntity> actual = service.getActiveAlerts();

        // then
        assertEquals(0, actual.size());
        verify(alertEntityRepository).findAlertsWhoseAlertStatusIsNotDismissedAndClosed();
        verifyNoMoreInteractions(alertEntityRepository);
    }

    @Test
    void getAlertById_whenEntityExists_thenReturnsAlert() {
        // given
        AlertEntity expected = alert(20L, "AL-3001");
        when(alertEntityRepository.findById(20L)).thenReturn(Optional.of(expected));

        // when
        AlertEntity actual = service.getAlertById(20L);

        // then
        assertSame(expected, actual);
        verify(alertEntityRepository).findById(20L);
        verifyNoMoreInteractions(alertEntityRepository);
    }

    @Test
    void getAlertById_whenEntityDoesNotExist_thenReturnsNull() {
        // given
        when(alertEntityRepository.findById(21L)).thenReturn(Optional.empty());

        // when
        AlertEntity actual = service.getAlertById(21L);

        // then
        assertNull(actual);
        verify(alertEntityRepository).findById(21L);
        verifyNoMoreInteractions(alertEntityRepository);
    }

    @Test
    void getAlertById_whenIdIsNull_thenPassesNullToRepositoryAndReturnsNull() {
        // given
        when(alertEntityRepository.findById(null)).thenReturn(Optional.empty());

        // when
        AlertEntity actual = service.getAlertById(null);

        // then
        assertNull(actual);
        verify(alertEntityRepository).findById(null);
        verifyNoMoreInteractions(alertEntityRepository);
    }

    private AlertEntity alert(Long id, String alertCode) {
        AlertEntity alert = new AlertEntity();
        alert.setId(id);
        alert.setAlertCode(alertCode);
        return alert;
    }
}

