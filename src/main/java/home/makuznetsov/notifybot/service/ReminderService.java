package home.makuznetsov.notifybot.service;

import home.makuznetsov.notifybot.entity.Reminder;
import home.makuznetsov.notifybot.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;

    @Transactional
    public void save(Reminder reminder){
        if (reminderRepository.existsById(reminder.getId())){
            log.info("Reminder with id {} already exists", reminder.getId());
        } else {
            reminderRepository.save(reminder);
        }
    }

    @Transactional
    public void deleteById(Long reminderId){
        if (reminderRepository.existsById(reminderId)){
            reminderRepository.deleteById(reminderId);
        } else {
            log.info("Reminder with id {} does not exist", reminderId);
        }
    }
}
