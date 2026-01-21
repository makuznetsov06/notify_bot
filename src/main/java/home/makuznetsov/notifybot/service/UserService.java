package home.makuznetsov.notifybot.service;

import home.makuznetsov.notifybot.entity.User;
import home.makuznetsov.notifybot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void save(User user) {
        if (user != null) {
            if (userRepository.findByTelegramUserId(user.getTelegramUserId()).isEmpty()) {
                userRepository.save(user);
            } else {
                log.info("User with telegramId {} already exists", user.getTelegramUserId());
            }
        }
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            userRepository.deleteById(userId);
        } else {
            log.info("Cannot delete user with Id {}", userId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramUserId(telegramId);
    }

    @Transactional
    public void updateNameByTelegramId(Long telegramUserId, String firstName){
        int updated = userRepository.updateNameByTelegramId(telegramUserId, firstName);
        if (updated > 0) {
            log.info("Updated name for user {}: {}", telegramUserId, firstName);
        } else {
            log.warn("User {} not found for name update", telegramUserId);
        }
    }

    @Transactional
    public void updateUserActivity(Long telegramUserId, boolean isActive){
        int updated = userRepository.updateActivityStatusByTelegramId(telegramUserId, isActive);
        if (updated > 0) {
            log.info("Updated activity status for user {}: {}", telegramUserId, isActive);
        } else {
            log.warn("User {} not found for activity update", telegramUserId);
        }
    }
}
