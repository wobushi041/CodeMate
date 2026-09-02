import {UserType} from "../models/user";

let currentUser: UserType | undefined;

const setCurrentUserState = (user: UserType) => {
    currentUser = user;
}

const getCurrentUserState = () : UserType | undefined => {
    return currentUser;
}

const clearCurrentUserState = () => {
    currentUser = undefined;
}

export {
    setCurrentUserState,
    getCurrentUserState,
    clearCurrentUserState,
}
