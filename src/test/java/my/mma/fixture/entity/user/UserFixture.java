package my.mma.fixture.entity.user;

import my.mma.api.user.entity.User;

public class UserFixture {

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static UserBuilder builderForPersist() {
        return new UserBuilder().id(null);
    }

    public static class UserBuilder {
        private Long id = 1L;
        private String email = "defaultemail123@google.com";
        private String nickname = "nickname123";
        private String role = "ROLE_USER";
        private String password = "pwd123";
        private String fcmToken = "abcdef";
        private int point = 300;
        private int earnedBetSucceedPoint = 200;

        private UserBuilder() {}

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UserBuilder point(int point) {
            this.point = point;
            return this;
        }

        public UserBuilder earnedBetSucceedPoint(int earnedBetSucceedPoint) {
            this.earnedBetSucceedPoint = earnedBetSucceedPoint;
            return this;
        }

        public User build() {
            return User.builder()
                    .id(id)
                    .email(email)
                    .nickname(nickname)
                    .role(role)
                    .password(password)
                    .fcmToken(fcmToken)
                    .point(point)
                    .earnedBetSucceedPoint(earnedBetSucceedPoint)
                    .build();
        }
    }

}
