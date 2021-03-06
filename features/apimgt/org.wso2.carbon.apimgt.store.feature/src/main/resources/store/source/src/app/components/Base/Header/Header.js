/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React from 'react'
import {Link, withRouter} from "react-router-dom";
import AuthManager from '../../../data/AuthManager.js';
import qs from 'qs'

import AppBar from 'material-ui/AppBar';
import Toolbar from 'material-ui/Toolbar';
import IconButton from 'material-ui/IconButton';
import Typography from 'material-ui/Typography';
import Button from 'material-ui/Button';
import Menu, { MenuItem } from 'material-ui/Menu';
import SearchIcon from 'material-ui-icons/Search';
import MenuIcon from 'material-ui-icons/Menu';
import PlaylistAddIcon from 'material-ui-icons/PlaylistAdd';
import CloseIcon from 'material-ui-icons/Close';
import TextField from 'material-ui/TextField';
import InfoIcon from 'material-ui-icons/Info';
import InfoLightBulb from 'material-ui-icons/LightbulbOutline';
import Avatar from 'material-ui/Avatar';
import List, {
    ListItem,
    ListItemIcon,
    ListItemText,
} from 'material-ui/List';

const helpTips = [
    "By API Name [Default]",
    "By API Provider [ Syntax - provider:xxxx ] or",
    "By API Version [ Syntax - version:xxxx ] or",
    "By Context [ Syntax - context:xxxx ] or",
    "By Description [ Syntax - description:xxxx ] or",
    "By Tags [ Syntax - tags:xxxx ] or",
    "By Sub-Context [ Syntax - subcontext:xxxx ] or",
    "By Documentation Content [ Syntax - doc:xxxx ]"
];

class Header extends React.Component {
    constructor(props){
        super(props);
        this.state = {
            anchorElUserMenu: undefined,
            anchorElAddMenu: undefined,
            anchorElMainMenu: undefined,
            anchorElTips: undefined,
            openUserMenu: false,
            openAddMenu: false,
            openMainMenu: false,
            searchVisible: false,
            openTips: false
        }
    }
    handleClickUserMenu = event => {
        this.setState({ openUserMenu: true, anchorElUserMenu: event.currentTarget });
    };

    handleRequestCloseUserMenu = () => {
        this.setState({ openUserMenu: false });
    };
    handleClickAddMenu = event => {
        this.setState({ openAddMenu: true, anchorElAddMenu: event.currentTarget });
    };

    handleRequestCloseAddMenu = () => {
        this.setState({ openAddMenu: false });
    };
    handleClickMainMenu = event => {
        this.setState({ openMainMenu: true, anchorElMainMenu: event.currentTarget });
    };

    handleRequestCloseMainMenu = () => {
        this.setState({ openMainMenu: false });
    };
    toggleSearch = () => {
        this.setState({searchVisible:!this.state.searchVisible});
    }
    handleClickTips = event => {
        this.setState({ openTips: true, anchorElTips: event.currentTarget });
    };
    handleRequestCloseTips = () => {
        this.setState({ openTips: false });
    };
    componentWillReceiveProps(nextProps){
        if(nextProps.showLeftMenu){
            this.setState({showLeftMenu:nextProps.showLeftMenu});
        }
    }

    render(props) {
        let user = AuthManager.getUser();
        const focusUsernameInputField = input => {
            input && input.focus();
        };
        return (
            <AppBar position="static">
                {this.state.searchVisible ?
                    <Toolbar>

                        <IconButton aria-label="Search" color="contrast">
                            <CloseIcon onClick={this.toggleSearch}/>
                        </IconButton>
                        <TextField
                            id="placeholder"
                            InputProps={{ placeholder: 'Placeholder' }}
                            helperText="Search By Name"
                            fullWidth
                            margin="normal"
                            color = "contrast"
                            inputRef={focusUsernameInputField}
                        />
                        <IconButton aria-label="Search Info" color="contrast">
                            <InfoLightBulb onClick={this.handleClickTips}/>
                        </IconButton>
                        <Menu
                            id="tip-menu"
                            anchorEl={this.state.anchorElTips}
                            open={this.state.openTips}
                            onRequestClose={this.handleRequestCloseTips}
                        >
                            <List dense={true}>
                                {helpTips.map((tip) => {
                                    return <ListItem button key={tip}>
                                        <ListItemIcon><InfoIcon /></ListItemIcon>
                                        <ListItemText  primary={tip} />
                                    </ListItem>
                                })}
                            </List>
                        </Menu>
                    </Toolbar>
                    :
                    <Toolbar>
                        {this.state.showLeftMenu ?
                        <IconButton color="contrast" aria-label="Menu">
                            <MenuIcon color="contrast" onClick={this.props.toggleDrawer}/>
                        </IconButton> : <span></span> }
                        <Typography type="title" color="inherit" style={{flex: 1}}>
                            <Link to="/" className="home">
                                    <img className="brand" src="/store/public/app/images/logo-inverse.svg" alt="wso2-logo"/>
                                    <span color="contrast" style={{fontSize: "15px", color:"#fff"}}>APIM Store</span>
                            </Link>
                        </Typography>
                        { user ?
                            <div style={{display:"flex"}}>
                                <Button aria-label="Search" onClick={this.toggleSearch} color="contrast">
                                    <SearchIcon />
                                </Button>

                                <Button aria-owns="simple-menu" aria-haspopup="true" onClick={this.handleClickMainMenu}
                                        color="contrast">
                                    <MenuIcon />
                                </Button>
                                <Menu
                                    id="simple-menu"
                                    anchorEl={this.state.anchorElMainMenu}
                                    open={this.state.openMainMenu}
                                    onRequestClose={this.handleRequestCloseMainMenu}
                                    style={{alignItems: "center", justifyContent: "center"}}
                                >

                                    <MenuItem onClick={this.handleRequestCloseMainMenu}>
                                        <Link to="/" style={{color: "#000", textDecoration: 'none'}}>List API</Link>
                                    </MenuItem>
                                    <MenuItem onClick={this.handleRequestCloseMainMenu}>
                                        <Link to="/applications" style={{color: "#000", textDecoration: 'none'}}>Applications</Link>
                                    </MenuItem>
                                </Menu>
                                {/* User menu */}
                                <Button aria-owns="simple-menu" aria-haspopup="true" onClick={this.handleClickUserMenu}
                                        color="contrast">
                                    <Avatar alt="{user.name}" src="/store/public/app/images/users/user.png"></Avatar>
                                    <span>{user.name}</span>
                                </Button>
                                <Menu
                                    id="simple-menu"
                                    anchorEl={this.state.anchorElUserMenu}
                                    open={this.state.openUserMenu}
                                    onRequestClose={this.handleRequestCloseUserMenu}
                                    style={{alignItems: "center", justifyContent: "center"}}
                                >
                                    <MenuItem onClick={this.handleRequestCloseUserMenu}>Change Password</MenuItem>

                                    <Link to="/logout">
                                        <MenuItem onClick={this.handleRequestCloseUserMenu}>Logout</MenuItem>
                                    </Link>
                                </Menu>
                            </div>
                            :
                            <div></div> }

                    </Toolbar>
                }
            </AppBar>
        );
    }
}


export default withRouter(Header)
